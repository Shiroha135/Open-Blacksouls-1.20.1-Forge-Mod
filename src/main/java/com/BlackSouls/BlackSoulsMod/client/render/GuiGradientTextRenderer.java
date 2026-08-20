package com.BlackSouls.BlackSoulsMod.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class GuiGradientTextRenderer {

    private GuiGradientTextRenderer() {
    }

    public static void draw(GuiGraphics graphics, Font font, String text, int x, int y, int[] palette) {
        if (text.isEmpty() || palette.length == 0) {
            return;
        }

        Matrix4f matrix = graphics.pose().last().pose();
        Vector4f start = matrix.transform(new Vector4f(x, y, 0.0F, 1.0F));
        Vector4f end = matrix.transform(new Vector4f(x + Math.max(1, font.width(text)), y, 0.0F, 1.0F));
        float minX = Math.min(start.x(), end.x());
        float width = Math.max(0.001F, Math.abs(end.x() - start.x()));
        float phase = (Util.getMillis() % 6000L) / 6000.0F;
        MultiBufferSource source = graphics.bufferSource();
        MultiBufferSource gradientSource = renderType -> new GradientVertexConsumer(
                source.getBuffer(renderType), minX, width, phase, palette
        );

        font.drawInBatch(text, x, y, 0xFFFFFFFF, false, matrix, gradientSource,
                Font.DisplayMode.NORMAL, 0, 15728880, false);
    }

    private static final class GradientVertexConsumer extends VertexConsumerWrapper {
        private final float minX;
        private final float width;
        private final float phase;
        private final int[] palette;
        private float vertexX;

        private GradientVertexConsumer(VertexConsumer parent, float minX, float width, float phase, int[] palette) {
            super(parent);
            this.minX = minX;
            this.width = width;
            this.phase = phase;
            this.palette = palette;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            vertexX = (float) x;
            return super.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            int color = sample((vertexX - minX) / width - phase, palette);
            return super.color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
        }
    }

    private static int sample(float position, int[] palette) {
        float normalized = position - (float) Math.floor(position);
        float scaled = normalized * palette.length;
        int index = Math.min(palette.length - 1, (int) scaled);
        int next = (index + 1) % palette.length;
        float progress = scaled - (float) Math.floor(scaled);
        progress = progress * progress * (3.0F - 2.0F * progress);
        return lerp(palette[index], palette[next], progress);
    }

    private static int lerp(int from, int to, float progress) {
        int red = Math.round((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * progress);
        int green = Math.round((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * progress);
        int blue = Math.round((from & 255) + ((to & 255) - (from & 255)) * progress);
        return red << 16 | green << 8 | blue;
    }
}
