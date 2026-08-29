package com.botamineecraft.taczsoldiers;

import com.botamineecraft.taczsoldiers.config.SoldiersConfig;
import com.botamineecraft.taczsoldiers.entity.SoldierEntity;
import com.botamineecraft.taczsoldiers.init.ModEntities;
import com.botamineecraft.taczsoldiers.init.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TaczSoldiersMod.MOD_ID)
public class TaczSoldiersMod {
    public static final String MOD_ID = "taczsoldiers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public TaczSoldiersMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::onEntityAttributeCreationEvent);
        modEventBus.addListener(this::onBuildCreativeModeTabContents);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SoldiersConfig.SPEC, "taczsoldiers-server.toml");

        LOGGER.info("TACZ Soldiers loaded. Soldiers will use TACZ guns when the 'tacz' mod is present.");
    }

    private void onEntityAttributeCreationEvent(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.SOLDIER.get(), SoldierEntity.createAttributes().build());
    }

    private void onBuildCreativeModeTabContents(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.SOLDIER_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.SOLDIER_SPAWN_EGG);
        }
    }
}
