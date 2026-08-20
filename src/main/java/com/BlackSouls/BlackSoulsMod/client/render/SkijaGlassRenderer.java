package com.BlackSouls.BlackSoulsMod.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector4i;
import org.slf4j.Logger;

public final class SkijaGlassRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean unavailable;
    private static boolean reported;

    private SkijaGlassRenderer() {
    }

    public static void prewarm() {
        if (unavailable) {
            return;
        }
        try {
            KawaseBlurRenderer.prewarm();
        } catch (Throwable throwable) {
            disable("Rounded shader prewarm failed.", throwable);
        }
    }

    public static boolean panel(GuiGraphics graphics, float x, float y, float width, float height,
                                float radius, float alpha, boolean highlighted, boolean blur) {
        return panel(graphics, x, y, width, height, radius, alpha, highlighted, blur, 0.78F);
    }

    public static boolean panel(GuiGraphics graphics, float x, float y, float width, float height,
                                float radius, float alpha, boolean highlighted, boolean blur,
                                float blurStrength) {
        if (unavailable || width <= 0.0F || height <= 0.0F || alpha <= 0.0F) {
            return false;
        }
        try {
            if (blur) {
                RenderTarget blurred = KawaseBlurRenderer.capture(graphics, blurStrength);
                if (blurred != null) {
                    renderBlur(graphics, blurred, x, y, width, height, radius);
                }
            }
            return renderGlass(graphics, x, y, width, height, radius, alpha, highlighted);
        } catch (Throwable throwable) {
            disable("SDF glass renderer disabled.", throwable);
            return false;
        }
    }

    public static boolean fill(GuiGraphics graphics, float x, float y, float width, float height,
                               float radius, Vector4i color) {
        return renderRounded(graphics, x, y, width, height, radius, 0.0F, false, color);
    }

    public static boolean stroke(GuiGraphics graphics, float x, float y, float width, float height,
                                 float radius, float thickness, Vector4i color) {
        return renderRounded(graphics, x, y, width, height, radius, thickness, true, color);
    }

    public static boolean blurOnly(GuiGraphics graphics, float x, float y, float width, float height,
                                   float radius, float blurStrength) {
        if (unavailable || width <= 0.0F || height <= 0.0F) {
            return false;
        }
        try {
            RenderTarget blurred = KawaseBlurRenderer.capture(graphics, blurStrength);
            if (blurred == null || ShaderHelper.noraMaskedBlurShader == null) {
                return false;
            }
            renderBlur(graphics, blurred, x, y, width, height, radius);
            return true;
        } catch (Throwable throwable) {
            disable("SDF blur-only renderer disabled.", throwable);
            return false;
        }
    }

    private static void renderBlur(GuiGraphics graphics, RenderTarget blurred,
                                   float x, float y, float width, float height, float radius) {
        ShaderInstance shader = ShaderHelper.noraMaskedBlurShader;
        if (shader == null) {
            return;
        }
        prepare();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, blurred.getColorTextureId());
        shader.setSampler("Sampler0", blurred);
        shader.safeGetUniform("FrameSize").set(
                (float) Minecraft.getInstance().getWindow().getWidth(),
                (float) Minecraft.getInstance().getWindow().getHeight()
        );
        shader.safeGetUniform("BlurAlpha").set(1.0F);
        setRoundedUniforms(shader, width, height, radius);
        drawQuad(graphics.pose().last().pose(), x, y, width, height,
                new Vector4i(255, 255, 255, 255));
        finish();
    }

    private static boolean renderGlass(GuiGraphics graphics, float x, float y, float width, float height,
                                       float radius, float alpha, boolean highlighted) {
        ShaderInstance shader = ShaderHelper.noraSkijaGlassShader;
        if (shader == null) {
            return false;
        }
        prepare();
        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("Alpha").set(Math.max(0.0F, Math.min(1.0F, alpha)));
        shader.safeGetUniform("Highlighted").set(highlighted ? 1.0F : 0.0F);
        setRoundedUniforms(shader, width, height, radius);
        drawQuad(graphics.pose().last().pose(), x, y, width, height,
                new Vector4i(255, 255, 255, 255));
        finish();
        return true;
    }

    private static boolean renderRounded(GuiGraphics graphics, float x, float y, float width, float height,
                                         float radius, float thickness, boolean strokeOnly, Vector4i color) {
        if (unavailable || width <= 0.0F || height <= 0.0F || color.w <= 0) {
            return false;
        }
        ShaderInstance shader = ShaderHelper.noraRoundedRectShader;
        if (shader == null) {
            return false;
        }
        try {
            prepare();
            RenderSystem.setShader(() -> shader);
            setRoundedUniforms(shader, width, height, radius);
            shader.safeGetUniform("StrokeWidth").set(Math.max(0.0F, thickness));
            shader.safeGetUniform("StrokeOnly").set(strokeOnly ? 1.0F : 0.0F);
            drawQuad(graphics.pose().last().pose(), x, y, width, height, color);
            finish();
            return true;
        } catch (Throwable throwable) {
            disable("SDF rounded rectangle renderer disabled.", throwable);
            return false;
        }
    }

    private static void setRoundedUniforms(ShaderInstance shader, float width, float height, float radius) {
        shader.safeGetUniform("PanelSize").set(width, height);
        shader.safeGetUniform("Radius").set(Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F)));
    }

    private static void prepare() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
    }

    private static void finish() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawQuad(Matrix4f matrix, float x, float y, float width, float height, Vector4i color) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        vertex(buffer, matrix, x, y, 0.0F, 0.0F, color);
        vertex(buffer, matrix, x, y + height, 0.0F, 1.0F, color);
        vertex(buffer, matrix, x + width, y + height, 1.0F, 1.0F, color);
        vertex(buffer, matrix, x + width, y, 1.0F, 0.0F, color);
        BufferUploader.drawWithShader(buffer.end());
    }

    private static void disable(String message, Throwable throwable) {
        unavailable = true;
        if (!reported) {
            reported = true;
            LOGGER.error(message, throwable);
        }
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y,
                               float u, float v, Vector4i color) {
        buffer.vertex(matrix, x, y, 0.0F)
                .uv(u, v)
                .color(color.x, color.y, color.z, color.w)
                .endVertex();
    }
}
