package com.botamineecraft.taczsoldiers.client;

import com.botamineecraft.taczsoldiers.TaczSoldiersMod;
import com.botamineecraft.taczsoldiers.init.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczSoldiersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TaczSoldiersClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SOLDIER.get(), SoldierRenderer::new);
    }

    private TaczSoldiersClientEvents() {
    }
}
