package com.BlackSouls.BlackSoulsMod.client.render;

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
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class KawaseBlurRenderer {
    private static final int[][] ITERATION_TABLE = {
            {1, 125}, {1, 225}, {2, 200}, {2, 300}, {2, 425},
            {3, 250}, {3, 325}, {3, 425}, {3, 550}, {4, 325},
            {4, 400}, {4, 500}, {4, 600}, {4, 725}, {4, 825},
            {5, 450}, {5, 525}, {5, 625}, {5, 725}, {5, 850}
    };
    private static final TextureTarget[] TARGETS = new TextureTarget[6];
    private static TextureTarget captureTarget;
    private static int cachedWidth;
    private static int cachedHeight;
    private static int cachedStrengthIndex = -1;
    private static long frameSerial;
    private static long cachedFrameSerial = -1L;

    private KawaseBlurRenderer() {
    }

    public static void prewarm() {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (ShaderHelper.noraKawaseDownShader == null
                || ShaderHelper.noraKawaseUpShader == null
                || mainTarget.width <= 0
                || mainTarget.height <= 0) {
            return;
        }
        ensureTargets(mainTarget.width, mainTarget.height);
    }

    public static void beginFrame() {
        frameSerial++;
        if (frameSerial == Long.MAX_VALUE) {
            frameSerial = 1L;
            cachedFrameSerial = -1L;
        }
    }

    public static RenderTarget capture(GuiGraphics graphics, float strength) {
        int index = Math.round(Mth.clamp(strength, 0.0F, 1.0F) * 19.0F);
        int clampedIndex = Mth.clamp(index, 0, ITERATION_TABLE.length - 1);
        if (!isCached(clampedIndex)) {
            graphics.flush();
        }
        return capture(clampedIndex);
    }

    @SuppressWarnings("unused")
    public static RenderTarget capture(float strength) {
        int index = Math.round(Mth.clamp(strength, 0.0F, 1.0F) * 19.0F);
        return capture(index);
    }

    public static RenderTarget capture(int strengthIndex) {
        ShaderInstance downShader = ShaderHelper.noraKawaseDownShader;
        ShaderInstance upShader = ShaderHelper.noraKawaseUpShader;
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (downShader == null || upShader == null || mainTarget.width <= 0 || mainTarget.height <= 0) {
            return null;
        }

        int[] strength = ITERATION_TABLE[Mth.clamp(strengthIndex, 0, ITERATION_TABLE.length - 1)];
        int clampedStrengthIndex = Mth.clamp(strengthIndex, 0, ITERATION_TABLE.length - 1);
        int iterations = Mth.clamp(strength[0], 1, TARGETS.length - 1);
        float offset = strength[1] / 100.0F;
        ensureTargets(mainTarget.width, mainTarget.height);
        if (isCached(clampedStrengthIndex)) {
            return TARGETS[0];
        }

        int previousFramebuffer = GlStateManager.getBoundFramebuffer();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            blit(mainTarget, captureTarget);
            renderPass(downShader, captureTarget, TARGETS[0], offset);
            for (int i = 0; i < iterations; i++) {
                renderPass(downShader, TARGETS[i], TARGETS[i + 1], offset);
            }
            for (int i = iterations; i > 0; i--) {
                renderPass(upShader, TARGETS[i], TARGETS[i - 1], offset);
            }
            cachedStrengthIndex = clampedStrengthIndex;
            cachedFrameSerial = frameSerial;
            return TARGETS[0];
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GlStateManager._viewport(0, 0, mainTarget.viewWidth, mainTarget.viewHeight);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void ensureTargets(int width, int height) {
        if (cachedWidth != width || cachedHeight != height) {
            cachedStrengthIndex = -1;
            cachedFrameSerial = -1L;
        }
        if (captureTarget == null) {
            captureTarget = createTarget(width, height);
        } else if (cachedWidth != width || cachedHeight != height) {
            captureTarget.resize(width, height, Minecraft.ON_OSX);
            captureTarget.setFilterMode(GL11.GL_LINEAR);
        }
        for (int i = 0; i < TARGETS.length; i++) {
            int divisor = 1 << i;
            int targetWidth = Math.max(1, width / divisor);
            int targetHeight = Math.max(1, height / divisor);
            if (TARGETS[i] == null) {
                TARGETS[i] = createTarget(targetWidth, targetHeight);
            } else if (TARGETS[i].width != targetWidth || TARGETS[i].height != targetHeight) {
                TARGETS[i].resize(targetWidth, targetHeight, Minecraft.ON_OSX);
                TARGETS[i].setFilterMode(GL11.GL_LINEAR);
            }
        }
        cachedWidth = width;
        cachedHeight = height;
    }

    private static boolean isCached(int strengthIndex) {
        return frameSerial > 0L
                && cachedFrameSerial == frameSerial
                && cachedStrengthIndex == strengthIndex;
    }

    private static TextureTarget createTarget(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.setFilterMode(GL11.GL_LINEAR);
        return target;
    }

    private static void blit(RenderTarget source, RenderTarget target) {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._glBlitFrameBuffer(
                0, 0, source.width, source.height,
                0, 0, target.width, target.height,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST
        );
    }

    private static void renderPass(ShaderInstance shader, RenderTarget source,
                                   RenderTarget target, float offset) {
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, source.getColorTextureId());
        shader.setSampler("Sampler0", source);
        shader.safeGetUniform("HalfTexelSize").set(0.5F / target.width, 0.5F / target.height);
        shader.safeGetUniform("Offset").set(offset);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(-1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(-1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    private static final class Mth {
        private Mth() {
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        @SuppressWarnings("SameParameterValue")
        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
