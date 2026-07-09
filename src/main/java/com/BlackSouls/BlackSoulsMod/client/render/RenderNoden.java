package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.entity.EntityNoden;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderNoden extends MobRenderer<EntityNoden, RenderNoden.NodenModel> {

    private static final ResourceLocation NODEN_TEXTURE = new ResourceLocation("blacksouls", "textures/entity/noden.png");

    public RenderNoden(EntityRendererProvider.Context context) {
        super(context, new NodenModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityNoden entity) {
        return NODEN_TEXTURE;
    }

    @Override
    protected void setupRotations(EntityNoden entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);

        if (entity.isSitting()) {
            poseStack.translate(0.0D, -0.5D, 0.0D);
        }
    }

    public static class NodenModel extends PlayerModel<EntityNoden> {

        public NodenModel(net.minecraft.client.model.geom.ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(EntityNoden entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.riding = entity.isSitting();
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        }
    }
}