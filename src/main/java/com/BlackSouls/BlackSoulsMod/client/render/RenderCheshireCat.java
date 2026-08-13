package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityCheshireCat;
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

public final class RenderCheshireCat extends EntityRenderer<EntityCheshireCat> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            BlackSouls.MODID, "textures/entity/cheshire_cat.png");

    public RenderCheshireCat(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.25F;
    }

    @Override
    public void render(EntityCheshireCat entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isInvisible()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.06D, 0.0D);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        int frame = Math.floorMod((entity.tickCount + (int) partialTicks) / 8, 3);
        float u0 = frame / 3.0F;
        float u1 = (frame + 1) / 3.0F;
        float v0 = 0.0F;
        float v1 = 0.25F;
        float height = 1.2F;
        float halfWidth = height * (44.0F / 48.0F) * 0.5F;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
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
    public ResourceLocation getTextureLocation(EntityCheshireCat entity) {
        return TEXTURE;
    }
}
