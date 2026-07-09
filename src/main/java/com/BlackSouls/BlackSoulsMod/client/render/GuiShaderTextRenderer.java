package com.BlackSouls.BlackSoulsMod.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public class GuiShaderTextRenderer {

    private static TextureTarget textMaskTarget;

    public static void renderRainbowText(GuiGraphics guiGraphics, Font font, Component text, int x, int y) {
        renderTextWithShader(guiGraphics, font, text, x, y, ShaderHelper.rainbowTextShader);
    }

    public static void renderSpongeNameText(GuiGraphics guiGraphics, Font font, Component text, int x, int y) {
        renderTextWithShader(guiGraphics, font, text, x, y, ShaderHelper.spongeNameShader);
    }

    private static void renderTextWithShader(GuiGraphics guiGraphics, Font font, Component text, int x, int y, ShaderInstance shader) {
        Minecraft mc = Minecraft.getInstance();
        if (shader == null) {
            guiGraphics.drawString(font, text, x, y, 0xFFFFFF, false);
            return;
        }

        String renderedText = text.getString();
        int textWidth = Math.max(1, font.width(renderedText));
        int textHeight = Math.max(1, font.lineHeight);
        int targetWidth = textWidth + 4;
        int targetHeight = textHeight + 4;

        renderTextMask(mc, font, text, targetWidth, targetHeight);
        if (textMaskTarget == null) {
            guiGraphics.drawString(font, text, x, y, 0xFFFFFF, false);
            return;
        }

        guiGraphics.flush();

        float time = mc.level != null ? (mc.level.getGameTime() + mc.getFrameTime()) * 0.04F : (System.currentTimeMillis() % 100000L) / 1000.0F;
        if (shader.safeGetUniform("GameTime") != null) {
            shader.safeGetUniform("GameTime").set(time);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, textMaskTarget.getColorTextureId());

        Matrix4f matrix = guiGraphics.pose().last().pose();
        float minX = x - 2.0F;
        float minY = y - 2.0F;
        float maxX = minX + targetWidth;
        float maxY = minY + targetHeight;

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, minX, maxY, 0.0F).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(matrix, maxX, maxY, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, maxX, minY, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, minX, minY, 0.0F).uv(0.0F, 0.0F).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.disableBlend();
    }

    private static void renderTextMask(Minecraft mc, Font font, Component text, int width, int height) {
        RenderTarget mainTarget = mc.getMainRenderTarget();
        if (mainTarget == null) {
            return;
        }

        ensureTarget(width, height);
        if (textMaskTarget == null) {
            return;
        }

        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        PoseStack modelViewStack = RenderSystem.getModelViewStack();

        textMaskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        textMaskTarget.clear(Minecraft.ON_OSX);
        textMaskTarget.bindWrite(true);

        RenderSystem.viewport(0, 0, width, height);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, width, height, 0.0F, 1000.0F, 3000.0F), VertexSorting.ORTHOGRAPHIC_Z);
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        modelViewStack.translate(0.0F, 0.0F, -2000.0F);
        RenderSystem.applyModelViewMatrix();

        GuiGraphics offscreenGraphics = new GuiGraphics(mc, MultiBufferSource.immediate(Tesselator.getInstance().getBuilder()));
        offscreenGraphics.drawString(font, text, 2, 2, 0xFFFFFFFF, false);
        offscreenGraphics.flush();

        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        mainTarget.bindWrite(true);
        RenderSystem.viewport(0, 0, mainTarget.width, mainTarget.height);
    }

    private static void ensureTarget(int width, int height) {
        if (textMaskTarget == null) {
            textMaskTarget = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        }
        if (textMaskTarget.width != width || textMaskTarget.height != height) {
            textMaskTarget.resize(width, height, Minecraft.ON_OSX);
        }
    }

    public static void closeTargets() {
        if (textMaskTarget != null) {
            textMaskTarget.destroyBuffers();
            textMaskTarget = null;
        }
    }
}
