package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalCemeteryCat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public final class RenderOriginalCemeteryCat extends EntityRenderer<EntityOriginalCemeteryCat> {
    public RenderOriginalCemeteryCat(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
        shadowStrength = 0.0F;
    }

    @Override
    public boolean shouldRender(EntityOriginalCemeteryCat entity, Frustum frustum, double x, double y, double z) {
        return false;
    }

    @Override
    public void render(EntityOriginalCemeteryCat entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
    }

    @Override
    public ResourceLocation getTextureLocation(EntityOriginalCemeteryCat entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
