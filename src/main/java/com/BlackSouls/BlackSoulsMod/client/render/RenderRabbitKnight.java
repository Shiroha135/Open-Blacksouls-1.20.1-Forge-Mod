package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityRabbitKnight;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class RenderRabbitKnight extends EntityRenderer<EntityRabbitKnight> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            BlackSouls.MODID, "textures/entity/rabbit_knight.png");

    public RenderRabbitKnight(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.32F;
    }

    @Override
    public void render(EntityRabbitKnight entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isInvisible()) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float u0 = 1.0F / 3.0F;
        float u1 = 2.0F / 3.0F;
        float v0 = 0.25F;
        float v1 = 0.5F;
        float height = 1.4F;
        float halfWidth = height * 0.5F;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose.pose(), pose.normal(), packedLight, -halfWidth, 0.0F, u0, v1);
        vertex(consumer, pose.pose(), pose.normal(), packedLight, halfWidth, 0.0F, u1, v1);
        vertex(consumer, pose.pose(), pose.normal(), packedLight, halfWidth, height, u1, v0);
        vertex(consumer, pose.pose(), pose.normal(), packedLight, -halfWidth, height, u0, v0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                               int light, float x, float y, float u, float v) {
        consumer.vertex(pose, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRabbitKnight entity) {
        return TEXTURE;
    }
}
