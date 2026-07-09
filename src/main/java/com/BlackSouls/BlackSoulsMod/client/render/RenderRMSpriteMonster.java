package com.BlackSouls.BlackSoulsMod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Monster;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderRMSpriteMonster<T extends Monster> extends EntityRenderer<T> {

    private final ResourceLocation texture;
    private final float renderHeight;
    private final float aspectRatio;

    public RenderRMSpriteMonster(EntityRendererProvider.Context context, ResourceLocation texture, float aspectRatio, float renderHeight, float shadowRadius) {
        super(context);
        this.texture = texture;
        this.aspectRatio = aspectRatio;
        this.renderHeight = renderHeight;
        this.shadowRadius = shadowRadius;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float width = this.renderHeight * this.aspectRatio;
        float halfWidth = width * 0.5F;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        vertex(consumer, poseMatrix, normalMatrix, packedLight, -halfWidth, 0.0F, 0.0F, 1.0F);
        vertex(consumer, poseMatrix, normalMatrix, packedLight, halfWidth, 0.0F, 1.0F, 1.0F);
        vertex(consumer, poseMatrix, normalMatrix, packedLight, halfWidth, this.renderHeight, 1.0F, 0.0F);
        vertex(consumer, poseMatrix, normalMatrix, packedLight, -halfWidth, this.renderHeight, 0.0F, 0.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light, float x, float y, float u, float v) {
        consumer.vertex(pose, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
