package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.entity.EntityRedHood;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class RenderRedHoodFallback extends MobRenderer<EntityRedHood, PlayerModel<EntityRedHood>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("blacksouls", "textures/entity/redhood.png");

    public RenderRedHoodFallback(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRedHood entity) {
        return TEXTURE;
    }
}
