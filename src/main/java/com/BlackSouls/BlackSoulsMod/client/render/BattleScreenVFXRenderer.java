package com.BlackSouls.BlackSoulsMod.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class BattleScreenVFXRenderer {
    private static final long FRAME_DURATION_MILLIS = 66L;
    private static final int CELL_SIZE = 192;

    public static boolean render(GuiGraphics graphics, int animationId, long startedAt,
                                 int centerX, int centerY, float canvasScale) {
        VFXAnimation animation = AnimationRegistry.ANIMATIONS.get(animationId);
        if (animation == null || animation.frames.isEmpty()) {
            return false;
        }
        int frameIndex = (int) ((System.currentTimeMillis() - startedAt) / FRAME_DURATION_MILLIS);
        if (frameIndex < 0 || frameIndex >= animation.frames.size()) {
            return false;
        }

        VFXFrame frame = animation.frames.get(frameIndex);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();

        for (VFXCell cell : frame.cells) {
            boolean textureTwo = cell.textureIndex >= 100;
            int pattern = textureTwo ? cell.textureIndex - 100 : cell.textureIndex;
            ResourceLocation texture = textureTwo ? animation.texture2 : animation.texture1;
            int rows = textureTwo ? animation.rows2 : animation.rows1;
            if (texture == null || rows <= 0) {
                continue;
            }

            int column = pattern % animation.cols;
            int row = pattern / animation.cols;
            int width = Math.max(1, Math.round(CELL_SIZE * cell.scaleX * 2.0F * canvasScale));
            int height = Math.max(1, Math.round(CELL_SIZE * cell.scaleY * 2.0F * canvasScale));
            float x = centerX + cell.offsetX * 32.0F * canvasScale;
            float y = centerY + cell.offsetY * 32.0F * canvasScale;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 220.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(cell.rotation));
            if (cell.mirror) {
                graphics.pose().scale(-1.0F, 1.0F, 1.0F);
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, cell.alpha);
            graphics.blit(texture, -width / 2, -height / 2, width, height,
                    column * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE,
                    animation.cols * CELL_SIZE, rows * CELL_SIZE);
            graphics.pose().popPose();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        return true;
    }

    private BattleScreenVFXRenderer() {
    }
}
