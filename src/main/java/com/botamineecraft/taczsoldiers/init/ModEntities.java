package com.botamineecraft.taczsoldiers.init;

import com.botamineecraft.taczsoldiers.TaczSoldiersMod;
import com.botamineecraft.taczsoldiers.entity.SoldierEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TaczSoldiersMod.MOD_ID);

    public static final RegistryObject<EntityType<SoldierEntity>> SOLDIER = ENTITY_TYPES.register("soldier",
            () -> EntityType.Builder.of(SoldierEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(new ResourceLocation(TaczSoldiersMod.MOD_ID, "soldier").toString()));

    private ModEntities() {
    }
}
