package com.botamineecraft.taczsoldiers.client;

import com.botamineecraft.taczsoldiers.TaczSoldiersMod;
import com.botamineecraft.taczsoldiers.entity.SoldierEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Humanoid soldier renderer. The held TACZ gun is rendered by TACZ itself:
 * TACZ replaces the rendering of gun items for any living entity holding one,
 * so the {@link ItemInHandLayer} is all that is needed here.
 */
public class SoldierRenderer extends MobRenderer<SoldierEntity, PlayerModel<SoldierEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TaczSoldiersMod.MOD_ID, "textures/entity/soldier.png");

    public SoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity entity) {
        return TEXTURE;
    }
}
