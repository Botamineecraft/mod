package com.botamineecraft.taczsoldiers.entity;

import com.botamineecraft.taczsoldiers.config.SoldiersConfig;
import com.botamineecraft.taczsoldiers.entity.ai.SoldierGuardGoal;
import com.botamineecraft.taczsoldiers.entity.ai.SoldierGunAttackGoal;
import com.botamineecraft.taczsoldiers.tacz.TaczBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A soldier that fights hostile mobs with TACZ guns.
 * <p>
 * With TACZ installed the soldier holds a real TACZ gun and fires it through
 * TACZ's {@code IGunOperator} API (bullets, sounds and reloads are fully TACZ).
 * Without TACZ the soldier falls back to shooting plain arrows.
 */
public class SoldierEntity extends PathfinderMob {
    private static final String TAG_GUARD_MODE = "GuardMode";
    private static final String TAG_GUARD_POS = "GuardPos";
    private static final String TAG_FALLBACK_ARMED = "FallbackArmed";

    private boolean guardMode = false;
    @Nullable private BlockPos guardPos = null;
    /** True when TACZ is missing and the soldier uses the arrow fallback. */
    private boolean fallbackArmed = false;

    private boolean gunDrawn = false;
    private ItemStack lastDrawnGun = ItemStack.EMPTY;
    private int fallbackShootCooldown = 0;

    public SoldierEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_CACTUS, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, SoldiersConfig.SOLDIER_MAX_HEALTH.get())
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SoldierGunAttackGoal(this));
        this.goalSelector.addGoal(2, new SoldierGuardGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return !SoldierEntity.this.isGuarding() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                target -> SoldiersConfig.TARGET_CREEPERS.get() || !(target instanceof Creeper)));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if (reason != MobSpawnType.LOAD) {
            this.equip();
        }
        return data;
    }

    private void equip() {
        if (TaczBridge.isTaczLoaded() && TaczBridge.ensureInitialized()) {
            String gunId = TaczBridge.pickRandomGunId(this.random);
            if (gunId != null) {
                ItemStack gun = TaczBridge.createGunStack(gunId);
                if (!gun.isEmpty()) {
                    this.setItemInHand(InteractionHand.MAIN_HAND, gun);
                    this.setDropChance(EquipmentSlot.MAINHAND, 0.35F);
                    this.gunDrawn = false;
                }
            }
        } else {
            this.fallbackArmed = true;
        }
    }

    /** True when the soldier can shoot: holds a TACZ gun, or uses the arrow fallback. */
    public boolean isArmed() {
        if (TaczBridge.isTaczLoaded()) {
            return TaczBridge.isGunItem(this.getMainHandItem());
        }
        return this.fallbackArmed;
    }

    public boolean isGuarding() {
        return this.guardMode && this.guardPos != null;
    }

    @Nullable
    public BlockPos getGuardPos() {
        return this.guardPos;
    }

    public void toggleGuardMode(Player player) {
        this.guardMode = !this.guardMode;
        this.guardPos = this.guardMode ? this.blockPosition() : null;
        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    this.guardMode ? "message.taczsoldiers.guard_on" : "message.taczsoldiers.guard_off"), true);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            ItemStack held = player.getItemInHand(hand);

            // Hand the soldier a (different) TACZ gun
            if (TaczBridge.isGunItem(held)) {
                ItemStack oldGun = this.getMainHandItem().copy();
                ItemStack newGun = held.copy();
                newGun.setCount(1);
                this.setItemInHand(InteractionHand.MAIN_HAND, newGun);
                held.shrink(1);
                if (!oldGun.isEmpty() && !player.addItem(oldGun)) {
                    this.spawnAtLocation(oldGun);
                }
                this.gunDrawn = false;
                this.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.0F);
                return InteractionResult.SUCCESS;
            }

            // Sneak + empty hand: toggle guard position
            if (player.isSecondaryUseActive() && held.isEmpty()) {
                this.toggleGuardMode(player);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        ItemStack mainHand = this.getMainHandItem();

        if (TaczBridge.isGunItem(mainHand)) {
            // (Re-)draw the gun whenever the held gun changes
            if (!this.gunDrawn || !ItemStack.matches(this.lastDrawnGun, mainHand)) {
                TaczBridge.initialData(this);
                TaczBridge.draw(this);
                this.gunDrawn = true;
                this.lastDrawnGun = mainHand.copy();
            }
            // Infinite ammo: keep the dummy-ammo reserve topped up
            if (SoldiersConfig.INFINITE_AMMO.get()) {
                CompoundTag tag = mainHand.getTag();
                if (tag == null || tag.getInt("DummyAmmo") < 100) {
                    TaczBridge.refillDummyAmmo(mainHand);
                }
            }
        } else {
            this.gunDrawn = false;
            this.lastDrawnGun = ItemStack.EMPTY;
        }

        if (this.fallbackShootCooldown > 0) {
            this.fallbackShootCooldown--;
        }
    }

    /** Arrow fallback used when TACZ is not installed. */
    public void shootFallback(LivingEntity target, float inaccuracyDegrees) {
        if (this.fallbackShootCooldown > 0) {
            return;
        }
        this.fallbackShootCooldown = 12;
        Vec3 direction = new Vec3(
                target.getX() - this.getX(),
                (target.getY() + target.getEyeHeight() * 0.85D) - (this.getY() + this.getEyeHeight()),
                target.getZ() - this.getZ());
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * 180.0F / (float) Math.PI) - 90.0F;
        float pitch = (float) (-(Mth.atan2(direction.y, horizontal) * 180.0F / (float) Math.PI));

        net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(this.level(), this);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(4.0D);
        arrow.shootFromRotation(this, pitch, yaw, 0.0F, 2.4F, inaccuracyDegrees);
        this.level().addFreshEntity(arrow);
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.guardMode = tag.getBoolean(TAG_GUARD_MODE);
        if (tag.contains(TAG_GUARD_POS)) {
            this.guardPos = BlockPos.of(tag.getLong(TAG_GUARD_POS));
        } else {
            this.guardPos = null;
        }
        this.fallbackArmed = tag.getBoolean(TAG_FALLBACK_ARMED);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_GUARD_MODE, this.guardMode);
        if (this.guardPos != null) {
            tag.putLong(TAG_GUARD_POS, this.guardPos.asLong());
        }
        tag.putBoolean(TAG_FALLBACK_ARMED, this.fallbackArmed);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.isArmed();
    }
}
