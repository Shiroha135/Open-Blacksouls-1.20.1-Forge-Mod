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
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class BattleTransitionRenderer {
    public static void drawMask(GuiGraphics graphics, ResourceLocation texture,
                                int width, int height, float progress) {
        float value = Math.max(0.0F, Math.min(1.0F, progress));
        ShaderInstance shader = ShaderHelper.battleTransitionShader;
        if (shader == null) {
            graphics.fill(0, 0, width, height, Math.round(value * 255.0F) << 24);
            return;
        }

        shader.safeGetUniform("Progress").set(value);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(() -> shader);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, 0.0F, height, 300.0F).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(matrix, width, height, 300.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, width, 0.0F, 300.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, 0.0F, 0.0F, 300.0F).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.disableBlend();
    }

    private BattleTransitionRenderer() {
    }
}
