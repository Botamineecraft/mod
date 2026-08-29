package com.botamineecraft.taczsoldiers.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Spawn egg that lazily resolves its entity type, so it can be registered
 * before the entity type exists (DeferredRegister friendly).
 */
public class ModSpawnEggItem extends SpawnEggItem {
    private final Supplier<EntityType<? extends Mob>> typeSupplier;

    public ModSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> typeSupplier, int backgroundColor, int highlightColor, Properties properties) {
        super(null, backgroundColor, highlightColor, properties);
        this.typeSupplier = typeSupplier;
    }

    @Override
    public EntityType<?> getType(@Nullable CompoundTag tag) {
        return this.typeSupplier.get();
    }
}
