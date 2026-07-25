package com.BlackSouls.BlackSoulsMod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

public final class FadedBannerRenderer {
    public static void draw(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        draw(guiGraphics, left, top, right, bottom, 1.0F);
    }

    public static void draw(GuiGraphics guiGraphics, int left, int top, int right, int bottom, float opacity) {
        float clampedOpacity = Math.max(0.0F, Math.min(1.0F, opacity));
        ShaderInstance shader = ShaderHelper.fadedBannerShader;
        if (shader == null) {
            int alpha = Math.round(187.0F * clampedOpacity);
            guiGraphics.fill(left, top, right, bottom, alpha << 24);
            return;
        }

        shader.safeGetUniform("Opacity").set(clampedOpacity);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, left, bottom, 0.0F).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(matrix, right, bottom, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, right, top, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, left, top, 0.0F).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.disableBlend();
    }

    private FadedBannerRenderer() {
    }
}
