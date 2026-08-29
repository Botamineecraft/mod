package com.botamineecraft.taczsoldiers.init;

import com.botamineecraft.taczsoldiers.TaczSoldiersMod;
import com.botamineecraft.taczsoldiers.item.ModSpawnEggItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TaczSoldiersMod.MOD_ID);

    public static final RegistryObject<Item> SOLDIER_SPAWN_EGG = ITEMS.register("soldier_spawn_egg",
            () -> new ModSpawnEggItem(ModEntities.SOLDIER, 0x4B5320, 0xBDB76B, new Item.Properties()));

    private ModItems() {
    }
}
