package yuki135.blacksouls.client.render;

import yuki135.blacksouls.entity.EntityOriginalWhiteBear;
import yuki135.blacksouls.util.OriginalNpcTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

public final class WhiteBearSourceSprite {
    private WhiteBearSourceSprite() {
    }

    public static boolean usesNativeModel(EntityOriginalWhiteBear actor) {
        var state = actor.originalRouteState();
        return !actor.isErasedForCurrentVisit() && !state.getBoolean("Transparent") && actor.currentPage() != null
                && "熊".equals(state.getString("CharacterName")) && state.getInt("CharacterIndex") == 5
                && state.getInt("Direction") == 2 && state.getInt("Pattern") == 0 && state.getInt("Opacity") == 255;
    }

    public static ResourceLocation texture(EntityOriginalWhiteBear actor) {
        return OriginalNpcTextures.texture(OriginalNpcTextures.character(actor.getRouteCharacterName()));
    }

    public static void render(EntityOriginalWhiteBear actor, Quaternionf camera, PoseStack poses,
                              MultiBufferSource buffer, int light) {
        var state = actor.originalRouteState();
        var sheet = OriginalNpcTextures.character(state.getString("CharacterName"));
        int alpha = Math.max(0, Math.min(255, state.getInt("Opacity")));
        if (actor.isErasedForCurrentVisit() || actor.currentPage() == null || state.getBoolean("Transparent")
                || sheet == null || alpha == 0) return;
        ResourceLocation texture = OriginalNpcTextures.texture(sheet);
        if (texture == null) return;
        boolean single = sheet.get("single").getAsBoolean();
        int columns = single ? 3 : 12, rows = single ? 4 : 8;
        int character = single ? 0 : Math.max(0, Math.min(7, state.getInt("CharacterIndex")));
        int direction = state.getInt("Direction");
        int row = character / 4 * 4 + (direction == 4 ? 1 : direction == 6 ? 2 : direction == 8 ? 3 : 0);
        int column = character % 4 * 3 + Math.max(0, Math.min(2, state.getInt("Pattern")));
        float u0 = column / (float) columns, u1 = (column + 1) / (float) columns;
        float v0 = row / (float) rows, v1 = (row + 1) / (float) rows;
        float height = 1.0F, halfWidth = height * sheet.get("frameWidth").getAsFloat() / sheet.get("frameHeight").getAsFloat() * .5F;
        poses.pushPose();
        poses.mulPose(camera);
        poses.mulPose(Axis.YP.rotationDegrees(180.0F));
        var vertices = buffer.getBuffer(RenderType.entityTranslucent(texture));
        var pose = poses.last();
        vertex(vertices, pose, light, alpha, -halfWidth, 0, u0, v1);
        vertex(vertices, pose, light, alpha, halfWidth, 0, u1, v1);
        vertex(vertices, pose, light, alpha, halfWidth, height, u1, v0);
        vertex(vertices, pose, light, alpha, -halfWidth, height, u0, v0);
        poses.popPose();
    }

    private static void vertex(VertexConsumer output, PoseStack.Pose pose, int light, int alpha,
                               float x, float y, float u, float v) {
        output.vertex(pose.pose(), x, y, 0).color(255, 255, 255, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();
    }
}
