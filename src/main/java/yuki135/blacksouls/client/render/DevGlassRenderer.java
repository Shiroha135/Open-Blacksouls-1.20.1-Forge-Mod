package yuki135.blacksouls.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

public final class DevGlassRenderer {
    private static final TextureTarget[] TARGETS = new TextureTarget[4];
    private static TextureTarget captureTarget;
    private static boolean captured;

    private DevGlassRenderer() {
    }

    public static void beginFrame(GuiGraphics graphics) {
        captured = false;
        capture(graphics);
    }

    public static void release() {
        if (captureTarget != null) {
            captureTarget.destroyBuffers();
            captureTarget = null;
        }
        for (int i = 0; i < TARGETS.length; i++) {
            if (TARGETS[i] != null) {
                TARGETS[i].destroyBuffers();
                TARGETS[i] = null;
            }
        }
        captured = false;
    }

    public static boolean panel(GuiGraphics graphics, float x, float y, float width, float height,
                                float radius, float alpha, boolean highlighted) {
        if (width <= 0.0F || height <= 0.0F || alpha <= 0.0F) {
            return false;
        }
        try {
            RenderTarget blurred = capture(graphics);
            if (blurred != null) {
                renderBlur(graphics, blurred, x, y, width, height, radius);
            }
            return renderGlass(graphics, x, y, width, height, radius, alpha, highlighted);
        } catch (Throwable ignored) {
            graphics.fill(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), 0xB0182436);
            return false;
        }
    }

    public static void fill(GuiGraphics graphics, float x, float y, float width, float height, float radius, int color) {
        if (!renderRounded(graphics, x, y, width, height, radius, 0.0F, false, color)) {
            graphics.fill(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), color);
        }
    }

    public static void stroke(GuiGraphics graphics, float x, float y, float width, float height,
                              float radius, float thickness, int color) {
        if (!renderRounded(graphics, x, y, width, height, radius, thickness, true, color)) {
            int left = Math.round(x);
            int top = Math.round(y);
            int right = Math.round(x + width);
            int bottom = Math.round(y + height);
            graphics.fill(left, top, right, top + 1, color);
            graphics.fill(left, bottom - 1, right, bottom, color);
            graphics.fill(left, top, left + 1, bottom, color);
            graphics.fill(right - 1, top, right, bottom, color);
        }
    }

    private static RenderTarget capture(GuiGraphics graphics) {
        ShaderInstance downShader = ShaderHelper.noraKawaseDownShader;
        ShaderInstance upShader = ShaderHelper.noraKawaseUpShader;
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (downShader == null || upShader == null || mainTarget.width <= 0 || mainTarget.height <= 0) {
            return null;
        }
        if (captured) {
            return TARGETS[0];
        }
        graphics.flush();
        try {
            ensureTargets(mainTarget.width, mainTarget.height);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            blit(mainTarget, captureTarget);
            float offset = (float) Math.max(1.0D, minecraft.getWindow().getGuiScale() * 1.25D);
            renderPass(downShader, captureTarget, TARGETS[0], offset);
            for (int i = 1; i < TARGETS.length; i++) {
                renderPass(downShader, TARGETS[i - 1], TARGETS[i], offset);
            }
            for (int i = TARGETS.length - 1; i > 0; i--) {
                renderPass(upShader, TARGETS[i], TARGETS[i - 1], offset);
            }
            captured = true;
            return TARGETS[0];
        } finally {
            mainTarget.bindWrite(true);
            RenderSystem.viewport(0, 0, mainTarget.viewWidth, mainTarget.viewHeight);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void ensureTargets(int width, int height) {
        width = Math.max(1, width / 2);
        height = Math.max(1, height / 2);
        if (captureTarget == null) {
            captureTarget = createTarget(width, height);
        } else if (captureTarget.width != width || captureTarget.height != height) {
            captureTarget.resize(width, height, Minecraft.ON_OSX);
            captureTarget.setFilterMode(9729);
        }
        for (int i = 0; i < TARGETS.length; i++) {
            int targetWidth = Math.max(1, width >> i);
            int targetHeight = Math.max(1, height >> i);
            if (TARGETS[i] == null) {
                TARGETS[i] = createTarget(targetWidth, targetHeight);
            } else if (TARGETS[i].width != targetWidth || TARGETS[i].height != targetHeight) {
                TARGETS[i].resize(targetWidth, targetHeight, Minecraft.ON_OSX);
                TARGETS[i].setFilterMode(9729);
            }
        }
    }

    private static TextureTarget createTarget(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.setFilterMode(9729);
        return target;
    }

    private static void blit(RenderTarget source, RenderTarget target) {
        GlStateManager._glBindFramebuffer(36008, source.frameBufferId);
        GlStateManager._glBindFramebuffer(36009, target.frameBufferId);
        GlStateManager._glBlitFrameBuffer(
                0, 0, source.width, source.height,
                0, 0, target.width, target.height,
                16384, 9729
        );
    }

    private static void renderPass(ShaderInstance shader, RenderTarget source, RenderTarget target, float offset) {
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, source.getColorTextureId());
        shader.setSampler("Sampler0", source);
        if (shader.getUniform("HalfTexelSize") != null) {
            shader.getUniform("HalfTexelSize").set(0.5F / target.width, 0.5F / target.height);
        }
        if (shader.getUniform("Offset") != null) {
            shader.getUniform("Offset").set(offset);
        }
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(-1.0D, -1.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(1.0D, -1.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(-1.0D, 1.0D, 0.0D).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    private static void renderBlur(GuiGraphics graphics, RenderTarget blurred, float x, float y,
                                   float width, float height, float radius) {
        ShaderInstance shader = ShaderHelper.noraMaskedBlurShader;
        if (shader == null) {
            return;
        }
        prepare(graphics);
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, blurred.getColorTextureId());
        shader.setSampler("Sampler0", blurred);
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        if (shader.getUniform("FrameSize") != null) {
            shader.getUniform("FrameSize").set((float) mainTarget.width, (float) mainTarget.height);
        }
        if (shader.getUniform("BlurAlpha") != null) {
            shader.getUniform("BlurAlpha").set(1.0F);
        }
        setRoundedUniforms(shader, width, height, radius);
        drawQuad(graphics.pose().last().pose(), x, y, width, height, 0xFFFFFFFF);
        finish();
    }

    private static boolean renderGlass(GuiGraphics graphics, float x, float y, float width, float height,
                                       float radius, float alpha, boolean highlighted) {
        int opacity = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 168.0F);
        return renderRounded(graphics, x, y, width, height, radius, 0.0F, false,
                opacity << 24 | 0x142333);
    }

    private static boolean renderRounded(GuiGraphics graphics, float x, float y, float width, float height,
                                         float radius, float thickness, boolean strokeOnly, int color) {
        ShaderInstance shader = ShaderHelper.noraRoundedRectShader;
        if (shader == null || width <= 0.0F || height <= 0.0F || (color >>> 24) == 0) {
            return false;
        }
        prepare(graphics);
        RenderSystem.setShader(() -> shader);
        setRoundedUniforms(shader, width, height, radius);
        if (shader.getUniform("StrokeWidth") != null) {
            shader.getUniform("StrokeWidth").set(Math.max(0.0F, thickness));
        }
        if (shader.getUniform("StrokeOnly") != null) {
            shader.getUniform("StrokeOnly").set(strokeOnly ? 1.0F : 0.0F);
        }
        drawQuad(graphics.pose().last().pose(), x, y, width, height, color);
        finish();
        return true;
    }

    private static void setRoundedUniforms(ShaderInstance shader, float width, float height, float radius) {
        if (shader.getUniform("PanelSize") != null) {
            shader.getUniform("PanelSize").set(width, height);
        }
        if (shader.getUniform("Radius") != null) {
            shader.getUniform("Radius").set(Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F)));
        }
    }

    private static void prepare(GuiGraphics graphics) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
    }

    private static void finish() {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawQuad(Matrix4f matrix, float x, float y, float width, float height, int color) {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = (color >>> 24) / 255.0F;
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(matrix, x, y, 0.0F).uv(0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y + height, 0.0F).uv(0.0F, 1.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0.0F).uv(1.0F, 1.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y, 0.0F).uv(1.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }
}
