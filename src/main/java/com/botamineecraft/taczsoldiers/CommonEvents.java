package com.botamineecraft.taczsoldiers;

import com.botamineecraft.taczsoldiers.tacz.TaczBridge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczSoldiersMod.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Gun data packs are reloaded for every server session
        TaczBridge.invalidateCaches();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TaczBridge.invalidateCaches();
    }

    private CommonEvents() {
    }
}
