package yuki135.blacksouls.client.render;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.EntityRabbitKnight;
import yuki135.blacksouls.util.OriginalNpcTextures;
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
        int opacity = entity.getSpriteOpacity();
        if (entity.isInvisible() || opacity <= 0) {
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
        ResourceLocation texture = TEXTURE;
        if (entity.isOriginalSourceManaged()) {
            var state = entity.originalRouteState();
            var sheet = OriginalNpcTextures.character(state.getString("CharacterName"));
            if (sheet == null) { poseStack.popPose(); return; }
            texture = OriginalNpcTextures.texture(sheet);
            int columns = sheet.get("single").getAsBoolean() ? 3 : 12;
            int rows = sheet.get("single").getAsBoolean() ? 4 : 8;
            int character = sheet.get("single").getAsBoolean() ? 0 : state.getInt("CharacterIndex");
            int direction = state.getInt("Direction");
            int directionRow = direction == 4 ? 1 : direction == 6 ? 2 : direction == 8 ? 3 : 0;
            int pattern = Math.max(0, Math.min(2, state.getInt("Pattern")));
            int column = character % 4 * 3 + pattern;
            int row = character / 4 * 4 + directionRow;
            u0 = column / (float) columns; u1 = (column + 1) / (float) columns;
            v0 = row / (float) rows; v1 = (row + 1) / (float) rows;
            halfWidth = height * sheet.get("frameWidth").getAsFloat() / sheet.get("frameHeight").getAsFloat() * .5F;
        }
        shadowStrength = opacity / 255.0F;
        VertexConsumer consumer = buffer.getBuffer(entity.isOriginalSourceManaged()
                ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose.pose(), pose.normal(), packedLight, opacity, -halfWidth, 0.0F, u0, v1);
        vertex(consumer, pose.pose(), pose.normal(), packedLight, opacity, halfWidth, 0.0F, u1, v1);
        vertex(consumer, pose.pose(), pose.normal(), packedLight, opacity, halfWidth, height, u1, v0);
        vertex(consumer, pose.pose(), pose.normal(), packedLight, opacity, -halfWidth, height, u0, v0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                               int light, int opacity, float x, float y, float u, float v) {
        consumer.vertex(pose, x, y, 0.0F)
                .color(255, 255, 255, opacity)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRabbitKnight entity) {
        if (entity.isOriginalSourceManaged()) {
            var sheet = OriginalNpcTextures.character(entity.originalRouteState().getString("CharacterName"));
            ResourceLocation texture = OriginalNpcTextures.texture(sheet);
            if (texture != null) return texture;
        }
        return TEXTURE;
    }
}
