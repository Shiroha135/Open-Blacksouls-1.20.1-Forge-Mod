package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.story.OriginalFishingRodEntry;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

public final class FishingRodSourceSprite {
    private FishingRodSourceSprite() {
    }

    public static ResourceLocation texture(EntityOriginalNpc actor) {
        return OriginalNpcTextures.texture(OriginalNpcTextures.character(actor.getRouteCharacterName()));
    }

    public static void render(EntityOriginalNpc actor, Quaternionf camera, PoseStack poses,
                              MultiBufferSource buffer, int light) {
        var state = actor.originalMovementState();
        var sheet = OriginalNpcTextures.character(state.getString("CharacterName"));
        int alpha = Math.max(0, Math.min(255, state.getInt("Opacity")));
        if (!OriginalFishingRodEntry.handles(actor.getNpcId()) || actor.isErasedForCurrentVisit()
                || actor.currentPage() == null || state.getBoolean("Transparent") || sheet == null || alpha == 0) return;
        ResourceLocation texture = OriginalNpcTextures.texture(sheet);
        if (texture == null) return;
        boolean single = sheet.get("single").getAsBoolean();
        int columns = single ? 3 : 12, rows = single ? 4 : 8;
        int character = single ? 0 : Math.max(0, Math.min(7, state.getInt("CharacterIndex")));
        int direction = state.getInt("Direction");
        int pattern = Math.max(0, Math.min(3, state.getInt("Pattern")));
        if (pattern == 3) pattern = 1;
        int row = character / 4 * 4 + (direction == 4 ? 1 : direction == 6 ? 2 : direction == 8 ? 3 : 0);
        int column = character % 4 * 3 + pattern;
        float u0 = column / (float) columns, u1 = (column + 1) / (float) columns;
        float v0 = row / (float) rows, v1 = (row + 1) / (float) rows;
        float height = sheet.get("frameHeight").getAsFloat() * (1.9F / 32.0F);
        float halfWidth = sheet.get("frameWidth").getAsFloat() * (1.9F / 32.0F) * .5F;
        poses.pushPose();
        try {
            poses.mulPose(camera);
            poses.mulPose(Axis.YP.rotationDegrees(180.0F));
            var vertices = buffer.getBuffer(RenderType.entityTranslucent(texture));
            var pose = poses.last();
            vertex(vertices, pose, light, alpha, -halfWidth, 0, u0, v1);
            vertex(vertices, pose, light, alpha, halfWidth, 0, u1, v1);
            vertex(vertices, pose, light, alpha, halfWidth, height, u1, v0);
            vertex(vertices, pose, light, alpha, -halfWidth, height, u0, v0);
        } finally {
            poses.popPose();
        }
    }

    private static void vertex(VertexConsumer output, PoseStack.Pose pose, int light, int alpha,
                               float x, float y, float u, float v) {
        output.vertex(pose.pose(), x, y, 0).color(255, 255, 255, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();
    }
}
