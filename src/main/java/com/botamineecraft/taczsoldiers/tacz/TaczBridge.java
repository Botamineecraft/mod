package com.botamineecraft.taczsoldiers.tacz;

import com.botamineecraft.taczsoldiers.config.SoldiersConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Reflection bridge to TACZ (Timeless and Classics Zero).
 * <p>
 * Nothing in this mod imports TACZ classes directly, so the mod compiles and loads
 * without TACZ. When TACZ is present, soldiers use the public {@code IGunOperator}
 * API that TACZ applies to every {@link LivingEntity} through mixins:
 * <ul>
 * <li>{@code IGunOperator.fromLivingEntity(entity)} - obtain the operator</li>
 * <li>{@code operator.shoot(pitch, yaw)} - fire the gun held in the main hand</li>
 * <li>{@code operator.reload()} / {@code operator.draw(gunSupplier)} / {@code operator.initialData()}</li>
 * </ul>
 * Guns are created by giving TACZ's generic gun item the proper NBT
 * ({@code GunId}, {@code GunCurrentAmmoCount}, {@code DummyAmmo}, {@code MaxDummyAmmo}).
 * Dummy ammo lets TACZ reload the gun without requiring an inventory.
 */
public final class TaczBridge {
    private static final Logger LOGGER = LogManager.getLogger("taczsoldiers/TaczBridge");
    public static final String TACZ_MOD_ID = "tacz";

    // NBT tags used by TACZ gun items (stable across TACZ versions)
    private static final String TAG_GUN_ID = "GunId";
    private static final String TAG_AMMO_COUNT = "GunCurrentAmmoCount";
    private static final String TAG_BULLET_IN_BARREL = "HasBulletInBarrel";
    private static final String TAG_DUMMY_AMMO = "DummyAmmo";
    private static final String TAG_MAX_DUMMY_AMMO = "MaxDummyAmmo";

    private static boolean initialized = false;
    private static boolean initFailed = false;

    @Nullable private static Class<?> gunOperatorClass;
    @Nullable private static Method fromLivingEntityMethod;
    @Nullable private static Method shootMethod;
    @Nullable private static Method reloadMethod;
    @Nullable private static Method drawMethod;
    @Nullable private static Method initialDataMethod;
    @Nullable private static Class<?> iGunClass;
    @Nullable private static Method getCommonGunIndexMethod;

    private static final Set<String> LOGGED_ERRORS = new HashSet<>();
    @Nullable private static List<String> cachedValidGuns = null;

    private TaczBridge() {
    }

    public static boolean isTaczLoaded() {
        return ModList.get().isLoaded(TACZ_MOD_ID);
    }

    /**
     * Lazily resolves the TACZ API through reflection. Safe to call repeatedly;
     * resolution happens once on the server thread.
     */
    public static synchronized boolean ensureInitialized() {
        if (!isTaczLoaded()) {
            return false;
        }
        if (initialized) {
            return !initFailed;
        }
        initialized = true;
        try {
            gunOperatorClass = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            fromLivingEntityMethod = gunOperatorClass.getMethod("fromLivingEntity", LivingEntity.class);
            shootMethod = gunOperatorClass.getMethod("shoot", Supplier.class, Supplier.class);
            reloadMethod = gunOperatorClass.getMethod("reload");
            drawMethod = gunOperatorClass.getMethod("draw", Supplier.class);
            initialDataMethod = gunOperatorClass.getMethod("initialData");
            iGunClass = Class.forName("com.tacz.guns.api.item.IGun");

            Class<?> timelessApiClass = Class.forName("com.tacz.guns.api.TimelessAPI");
            for (Method method : timelessApiClass.getMethods()) {
                if (method.getName().equals("getCommonGunIndex") && method.getParameterCount() == 1) {
                    getCommonGunIndexMethod = method;
                    break;
                }
            }
            LOGGER.info("TACZ API bridge initialized successfully.");
            return true;
        } catch (Throwable throwable) {
            initFailed = true;
            LOGGER.error("TACZ is installed, but the TACZ Soldiers bridge failed to initialize. " +
                    "Soldiers will fall back to simple arrow shots. Cause: {}", throwable.toString());
            return false;
        }
    }

    /** @return true when the bridge works and the stack's item implements TACZ's IGun interface. */
    public static boolean isGunItem(ItemStack stack) {
        if (stack.isEmpty() || !ensureInitialized() || iGunClass == null) {
            return false;
        }
        return iGunClass.isInstance(stack.getItem());
    }

    /** Checks whether a TACZ gun id exists in the loaded gun data packs. */
    public static boolean isValidGunId(String gunIdString) {
        if (!ensureInitialized() || getCommonGunIndexMethod == null) {
            return false;
        }
        try {
            ResourceLocation gunId = ResourceLocation.tryParse(gunIdString);
            if (gunId == null) {
                return false;
            }
            Object result = getCommonGunIndexMethod.invoke(null, gunId);
            return result instanceof Optional<?> optional && optional.isPresent();
        } catch (Throwable throwable) {
            logOnce("isValidGunId", throwable);
            return false;
        }
    }

    /** Filters the configured gun pool down to ids that actually exist. Cached per server session. */
    public static List<String> getValidGuns() {
        if (cachedValidGuns != null) {
            return cachedValidGuns;
        }
        List<String> valid = new ArrayList<>();
        for (String configured : SoldiersConfig.GUN_POOL.get()) {
            if (isValidGunId(configured.trim())) {
                valid.add(configured.trim());
            } else {
                LOGGER.warn("Gun id '{}' from the gun_pool config is not available in TACZ, skipping.", configured);
            }
        }
        if (valid.isEmpty()) {
            LOGGER.error("No valid TACZ gun ids configured! Soldiers will not receive guns. Fix 'gun_pool' in taczsoldiers-server.toml.");
        }
        cachedValidGuns = valid;
        return valid;
    }

    /** Must be called when a new server starts (gun data is reloaded). */
    public static void invalidateCaches() {
        cachedValidGuns = null;
    }

    @Nullable
    public static String pickRandomGunId(net.minecraft.util.RandomSource random) {
        List<String> valid = getValidGuns();
        if (valid.isEmpty()) {
            return null;
        }
        return valid.get(random.nextInt(valid.size()));
    }

    /**
     * Creates a ready-to-use gun ItemStack for the given TACZ gun id,
     * loaded with a magazine of ammo and a dummy-ammo reserve for reloading.
     */
    public static ItemStack createGunStack(String gunIdString) {
        ResourceLocation gunId = ResourceLocation.tryParse(gunIdString);
        if (gunId == null) {
            return ItemStack.EMPTY;
        }
        // Modern TACZ: one generic gun item, gun selected via NBT
        Item modernGunItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(TACZ_MOD_ID, "modern_kinetic_gun"));
        if (modernGunItem != null && modernGunItem != Items.AIR) {
            ItemStack stack = new ItemStack(modernGunItem);
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(TAG_GUN_ID, gunId.toString());
            tag.putInt(TAG_AMMO_COUNT, 30);
            tag.putBoolean(TAG_BULLET_IN_BARREL, true);
            applyDummyAmmo(stack);
            return stack;
        }
        // Older TACZ: per-gun items registered under their gun id
        Item legacyGunItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(gunId.getNamespace(), gunId.getPath()));
        if (legacyGunItem != null && legacyGunItem != Items.AIR) {
            ItemStack stack = new ItemStack(legacyGunItem);
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt(TAG_AMMO_COUNT, 30);
            tag.putBoolean(TAG_BULLET_IN_BARREL, true);
            applyDummyAmmo(stack);
            return stack;
        }
        logOnce("createGunStack:" + gunIdString, new IllegalStateException("No TACZ gun item found for " + gunIdString));
        return ItemStack.EMPTY;
    }

    private static void applyDummyAmmo(ItemStack stack) {
        int reserve = SoldiersConfig.AMMO_RESERVE.get();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_DUMMY_AMMO, reserve);
        tag.putInt(TAG_MAX_DUMMY_AMMO, reserve);
    }

    /** Reads the ammo counter from the gun NBT (0 when absent). */
    public static int getAmmoCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        return tag.getInt(TAG_AMMO_COUNT) + (tag.getBoolean(TAG_BULLET_IN_BARREL) ? 1 : 0);
    }

    /** Refills the dummy-ammo reserve so the soldier can keep reloading forever. */
    public static void refillDummyAmmo(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int reserve = SoldiersConfig.AMMO_RESERVE.get();
        tag.putInt(TAG_DUMMY_AMMO, reserve);
        tag.putInt(TAG_MAX_DUMMY_AMMO, reserve);
    }

    /** @return the ShootResult name, or "BRIDGE_ERROR" / "BRIDGE_DISABLED" on failure. */
    public static String shoot(LivingEntity shooter) {
        if (!ensureInitialized() || gunOperatorClass == null || fromLivingEntityMethod == null || shootMethod == null) {
            return "BRIDGE_DISABLED";
        }
        try {
            Object operator = fromLivingEntityMethod.invoke(null, shooter);
            Supplier<Float> pitch = shooter::getXRot;
            Supplier<Float> yaw = shooter::getYRot;
            Object result = shootMethod.invoke(operator, pitch, yaw);
            return result == null ? "BRIDGE_ERROR" : result.toString();
        } catch (Throwable throwable) {
            logOnce("shoot", throwable);
            return "BRIDGE_ERROR";
        }
    }

    public static void reload(LivingEntity shooter) {
        if (!ensureInitialized() || fromLivingEntityMethod == null || reloadMethod == null) {
            return;
        }
        try {
            Object operator = fromLivingEntityMethod.invoke(null, shooter);
            reloadMethod.invoke(operator);
        } catch (Throwable throwable) {
            logOnce("reload", throwable);
        }
    }

    /** Tells TACZ the soldier drew the gun currently held in its main hand. */
    public static void draw(LivingEntity shooter) {
        if (!ensureInitialized() || fromLivingEntityMethod == null || drawMethod == null) {
            return;
        }
        try {
            Object operator = fromLivingEntityMethod.invoke(null, shooter);
            Supplier<ItemStack> gunSupplier = shooter::getMainHandItem;
            drawMethod.invoke(operator, gunSupplier);
        } catch (Throwable throwable) {
            logOnce("draw", throwable);
        }
    }

    public static void initialData(LivingEntity shooter) {
        if (!ensureInitialized() || fromLivingEntityMethod == null || initialDataMethod == null) {
            return;
        }
        try {
            Object operator = fromLivingEntityMethod.invoke(null, shooter);
            initialDataMethod.invoke(operator);
        } catch (Throwable throwable) {
            logOnce("initialData", throwable);
        }
    }

    private static void logOnce(String key, Throwable throwable) {
        if (LOGGED_ERRORS.add(key)) {
            Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
            LOGGER.error("TACZ bridge call '{}' failed: {}", key, cause.toString());
        }
    }
}
