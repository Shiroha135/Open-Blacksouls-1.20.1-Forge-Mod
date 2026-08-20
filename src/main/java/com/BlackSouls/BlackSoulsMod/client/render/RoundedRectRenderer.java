package com.BlackSouls.BlackSoulsMod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4i;

/**
 * 圆角矩形渲染器，1:1 复刻自 ArcaneVortex 的 RoundedRectRenderer。
 * 全部使用 BufferBuilder + POSITION_COLOR 格式（positionColorShader），
 * 不用 NEW_ENTITY，避开 Embeddium 严格的多元素校验。
 */
public final class RoundedRectRenderer {
    private RoundedRectRenderer() {
    }

    public static void renderRoundedRect(GuiGraphics graphics, float x, float y, float width, float height,
                                          float topLeft, float topRight, float bottomRight, float bottomLeft,
                                          Vector4i color, int segments) {
        renderRoundedRectGradient(graphics, x, y, width, height, topLeft, topRight, bottomRight, bottomLeft,
                null, color, segments);
    }

    public static void renderRoundedRectGradient(GuiGraphics graphics, float x, float y, float width, float height,
                                                  float topLeft, float topRight, float bottomRight, float bottomLeft,
                                                  GradientConfig gradient, Vector4i fallbackColor, int segments) {
        PoseStack poseStack = graphics.pose();
        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float maxRadius = Math.min(width, height) / 2.0f;
        topLeft = Math.min(topLeft, maxRadius);
        topRight = Math.min(topRight, maxRadius);
        bottomRight = Math.min(bottomRight, maxRadius);
        bottomLeft = Math.min(bottomLeft, maxRadius);
        float innerLeft = x + topLeft;
        float innerRight = x + width - topRight;
        float innerTop = y + topLeft;
        float innerBottom = y + height - bottomLeft;
        long time = System.currentTimeMillis();

        addQuadGradient(builder, matrix, innerLeft, innerTop, innerRight, innerBottom,
                x, y, width, height, gradient, fallbackColor, time);
        addQuadGradient(builder, matrix, x + topLeft, y, x + width - topRight, y + Math.max(topLeft, topRight),
                x, y, width, height, gradient, fallbackColor, time);
        addQuadGradient(builder, matrix, x + bottomLeft, y + height - Math.max(bottomLeft, bottomRight),
                x + width - bottomRight, y + height, x, y, width, height, gradient, fallbackColor, time);
        addQuadGradient(builder, matrix, x, y + topLeft, x + Math.max(topLeft, bottomLeft), y + height - bottomLeft,
                x, y, width, height, gradient, fallbackColor, time);
        addQuadGradient(builder, matrix, x + width - Math.max(topRight, bottomRight), y + topRight,
                x + width, y + height - bottomRight, x, y, width, height, gradient, fallbackColor, time);
        if (topLeft > 0.0f) {
            addRoundedCornerGradient(builder, matrix, x + topLeft, y + topLeft, topLeft,
                    0.0f, 180.0f, 270.0f, segments, x, y, width, height, gradient, fallbackColor, time);
        }
        if (topRight > 0.0f) {
            addRoundedCornerGradient(builder, matrix, x + width - topRight, y + topRight, topRight,
                    0.0f, 270.0f, 360.0f, segments, x, y, width, height, gradient, fallbackColor, time);
        }
        if (bottomRight > 0.0f) {
            addRoundedCornerGradient(builder, matrix, x + width - bottomRight, y + height - bottomRight, bottomRight,
                    0.0f, 0.0f, 90.0f, segments, x, y, width, height, gradient, fallbackColor, time);
        }
        if (bottomLeft > 0.0f) {
            addRoundedCornerGradient(builder, matrix, x + bottomLeft, y + height - bottomLeft, bottomLeft,
                    0.0f, 90.0f, 180.0f, segments, x, y, width, height, gradient, fallbackColor, time);
        }
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableBlend();
    }

    private static void addQuadGradient(BufferBuilder builder, Matrix4f matrix,
                                         float x1, float y1, float x2, float y2,
                                         float rectX, float rectY, float rectWidth, float rectHeight,
                                         GradientConfig gradient, Vector4i fallbackColor, long time) {
        int color1 = getColorAtPosition(x1, y1, rectX, rectY, rectWidth, rectHeight, gradient, fallbackColor, time);
        int color2 = getColorAtPosition(x1, y2, rectX, rectY, rectWidth, rectHeight, gradient, fallbackColor, time);
        int color3 = getColorAtPosition(x2, y2, rectX, rectY, rectWidth, rectHeight, gradient, fallbackColor, time);
        int color4 = getColorAtPosition(x2, y1, rectX, rectY, rectWidth, rectHeight, gradient, fallbackColor, time);
        addColoredVertex(builder, matrix, x1, y1, color1);
        addColoredVertex(builder, matrix, x1, y2, color2);
        addColoredVertex(builder, matrix, x2, y2, color3);
        addColoredVertex(builder, matrix, x1, y1, color1);
        addColoredVertex(builder, matrix, x2, y2, color3);
        addColoredVertex(builder, matrix, x2, y1, color4);
    }

    @SuppressWarnings("SameParameterValue")
    private static void addRoundedCornerGradient(BufferBuilder builder, Matrix4f matrix,
                                                  float centerX, float centerY, float outerRadius, float innerRadius,
                                                  float startAngle, float endAngle, int segments,
                                                  float rectX, float rectY, float rectWidth, float rectHeight,
                                                  GradientConfig gradient, Vector4i fallbackColor, long time) {
        float angleStep = (endAngle - startAngle) / (float) segments;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(startAngle + angleStep * (float) i);
            float angle2 = (float) Math.toRadians(startAngle + angleStep * (float) (i + 1));
            float x1Outer = centerX + (float) Math.cos(angle1) * outerRadius;
            float y1Outer = centerY + (float) Math.sin(angle1) * outerRadius;
            float x2Outer = centerX + (float) Math.cos(angle2) * outerRadius;
            float y2Outer = centerY + (float) Math.sin(angle2) * outerRadius;
            float x1Inner = centerX + (float) Math.cos(angle1) * innerRadius;
            float y1Inner = centerY + (float) Math.sin(angle1) * innerRadius;
            float x2Inner = centerX + (float) Math.cos(angle2) * innerRadius;
            float y2Inner = centerY + (float) Math.sin(angle2) * innerRadius;
            int color1Outer = getColorAtPosition(x1Outer, y1Outer, rectX, rectY, rectWidth, rectHeight, gradient, fallbackColor, time);
            int color1Inner = getColorAtPosition(x1Inner, y1Inner, rectX, rectY, rectWidth, rectHeight, gradient, fallbackColor, time);
            int color2Inner = getColorAtPosition(x2Inner, y2Inner, rectX, rectY, rectWidth, rectHeight, gradient, fallbackColor, time);
            int color2Outer = getColorAtPosition(x2Outer, y2Outer, rectX, rectY, rectWidth, rectHeight, gradient, fallbackColor, time);
            addColoredVertex(builder, matrix, x1Outer, y1Outer, color1Outer);
            addColoredVertex(builder, matrix, x1Inner, y1Inner, color1Inner);
            addColoredVertex(builder, matrix, x2Inner, y2Inner, color2Inner);
            addColoredVertex(builder, matrix, x1Outer, y1Outer, color1Outer);
            addColoredVertex(builder, matrix, x2Inner, y2Inner, color2Inner);
            addColoredVertex(builder, matrix, x2Outer, y2Outer, color2Outer);
        }
    }

    public static void renderRoundedRectBorderGradient(GuiGraphics graphics, float x, float y, float width, float height,
                                                        float topLeft, float topRight, float bottomRight, float bottomLeft,
                                                        float borderWidth, GradientConfig gradient, Vector4i fallbackColor, int segments) {
        PoseStack poseStack = graphics.pose();
        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float maxRadius = Math.min(width, height) / 2.0f;
        topLeft = Math.min(topLeft, maxRadius);
        topRight = Math.min(topRight, maxRadius);
        bottomRight = Math.min(bottomRight, maxRadius);
        bottomLeft = Math.min(bottomLeft, maxRadius);
        long time = System.currentTimeMillis();
        float perimeter = calculatePerimeter(width, height, topLeft, topRight, bottomRight, bottomLeft);
        float topLength = width - topLeft - topRight;
        int topSegments = Math.max(2, (int) (topLength / 5.0f));
        float topStart = 0.0f;
        addTopBorderSegmented(builder, matrix, x + topLeft, y, x + width - topRight, y, borderWidth, topSegments, topStart, topLength, perimeter, gradient, fallbackColor, time);
        float rightLength = height - topRight - bottomRight;
        int rightSegments = Math.max(2, (int) (rightLength / 5.0f));
        float rightStart = (float) ((double) topLength + Math.PI * (double) topRight / 2.0);
        addRightBorderSegmented(builder, matrix, x + width, y + topRight, x + width, y + height - bottomRight, borderWidth, rightSegments, rightStart, rightLength, perimeter, gradient, fallbackColor, time);
        float bottomLength = width - bottomLeft - bottomRight;
        int bottomSegments = Math.max(2, (int) (bottomLength / 5.0f));
        float bottomStart = (float) ((double) topLength + Math.PI * (double) topRight / 2.0 + (double) rightLength + Math.PI * (double) bottomRight / 2.0);
        addBottomBorderSegmented(builder, matrix, x + width - bottomRight, y + height, x + bottomLeft, y + height, borderWidth, bottomSegments, bottomStart, bottomLength, perimeter, gradient, fallbackColor, time);
        float leftLength = height - topLeft - bottomLeft;
        int leftSegments = Math.max(2, (int) (leftLength / 5.0f));
        float leftStart = (float) ((double) topLength + Math.PI * (double) topRight / 2.0 + (double) rightLength + Math.PI * (double) bottomRight / 2.0 + (double) bottomLength + Math.PI * (double) bottomLeft / 2.0);
        addLeftBorderSegmented(builder, matrix, x, y + height - bottomLeft, x, y + topLeft, borderWidth, leftSegments, leftStart, leftLength, perimeter, gradient, fallbackColor, time);
        if (topLeft > 0.0f) {
            addRoundedCornerBorderCircular(builder, matrix, x + topLeft, y + topLeft, topLeft, topLeft - borderWidth, 180.0f, 270.0f, segments,
                    (float) ((double) topLength + Math.PI * (double) topRight / 2.0 + (double) rightLength + Math.PI * (double) bottomRight / 2.0 + (double) bottomLength + Math.PI * (double) bottomLeft / 2.0 + (double) leftLength),
                    (float) (Math.PI * (double) topLeft / 2.0), perimeter, gradient, fallbackColor, time);
        }
        if (topRight > 0.0f) {
            addRoundedCornerBorderCircular(builder, matrix, x + width - topRight, y + topRight, topRight, topRight - borderWidth, 270.0f, 360.0f, segments,
                    topLength, (float) (Math.PI * (double) topRight / 2.0), perimeter, gradient, fallbackColor, time);
        }
        if (bottomRight > 0.0f) {
            addRoundedCornerBorderCircular(builder, matrix, x + width - bottomRight, y + height - bottomRight, bottomRight, bottomRight - borderWidth, 0.0f, 90.0f, segments,
                    (float) ((double) topLength + Math.PI * (double) topRight / 2.0 + (double) rightLength), (float) (Math.PI * (double) bottomRight / 2.0), perimeter, gradient, fallbackColor, time);
        }
        if (bottomLeft > 0.0f) {
            addRoundedCornerBorderCircular(builder, matrix, x + bottomLeft, y + height - bottomLeft, bottomLeft, bottomLeft - borderWidth, 90.0f, 180.0f, segments,
                    (float) ((double) topLength + Math.PI * (double) topRight / 2.0 + (double) rightLength + Math.PI * (double) bottomRight / 2.0 + (double) bottomLength), (float) (Math.PI * (double) bottomLeft / 2.0), perimeter, gradient, fallbackColor, time);
        }
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableBlend();
    }

    private static float calculatePerimeter(float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft) {
        float straightEdges = width - topLeft - topRight + (height - topRight - bottomRight) + (width - bottomRight - bottomLeft) + (height - bottomLeft - topLeft);
        float corners = (float) (Math.PI * (double) (topLeft + topRight + bottomRight + bottomLeft) / 2.0);
        return straightEdges + corners;
    }

    private static void addTopBorderSegmented(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2,
                                               float borderWidth, int segments, float startDistance, float edgeLength, float totalPerimeter,
                                               GradientConfig gradient, Vector4i fallbackColor, long time) {
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / (float) segments;
            float t2 = (float) (i + 1) / (float) segments;
            float px1 = x1 + (x2 - x1) * t1;
            float px2 = x1 + (x2 - x1) * t2;
            float dist1 = startDistance + edgeLength * t1;
            float dist2 = startDistance + edgeLength * t2;
            float progress1 = dist1 / totalPerimeter;
            float progress2 = dist2 / totalPerimeter;
            int color1 = getBorderColorAtProgress(progress1, gradient, fallbackColor, time);
            int color2 = getBorderColorAtProgress(progress2, gradient, fallbackColor, time);
            addColoredVertex(builder, matrix, px1, y1, color1);
            addColoredVertex(builder, matrix, px1, y1 + borderWidth, color1);
            addColoredVertex(builder, matrix, px2, y2 + borderWidth, color2);
            addColoredVertex(builder, matrix, px1, y1, color1);
            addColoredVertex(builder, matrix, px2, y2 + borderWidth, color2);
            addColoredVertex(builder, matrix, px2, y2, color2);
        }
    }

    private static void addRightBorderSegmented(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2,
                                                 float borderWidth, int segments, float startDistance, float edgeLength, float totalPerimeter,
                                                 GradientConfig gradient, Vector4i fallbackColor, long time) {
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / (float) segments;
            float t2 = (float) (i + 1) / (float) segments;
            float py1 = y1 + (y2 - y1) * t1;
            float py2 = y1 + (y2 - y1) * t2;
            float dist1 = startDistance + edgeLength * t1;
            float dist2 = startDistance + edgeLength * t2;
            float progress1 = dist1 / totalPerimeter;
            float progress2 = dist2 / totalPerimeter;
            int color1 = getBorderColorAtProgress(progress1, gradient, fallbackColor, time);
            int color2 = getBorderColorAtProgress(progress2, gradient, fallbackColor, time);
            addColoredVertex(builder, matrix, x1, py1, color1);
            addColoredVertex(builder, matrix, x1 - borderWidth, py1, color1);
            addColoredVertex(builder, matrix, x2 - borderWidth, py2, color2);
            addColoredVertex(builder, matrix, x1, py1, color1);
            addColoredVertex(builder, matrix, x2 - borderWidth, py2, color2);
            addColoredVertex(builder, matrix, x2, py2, color2);
        }
    }

    private static void addBottomBorderSegmented(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2,
                                                  float borderWidth, int segments, float startDistance, float edgeLength, float totalPerimeter,
                                                  GradientConfig gradient, Vector4i fallbackColor, long time) {
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / (float) segments;
            float t2 = (float) (i + 1) / (float) segments;
            float px1 = x1 + (x2 - x1) * t1;
            float px2 = x1 + (x2 - x1) * t2;
            float dist1 = startDistance + edgeLength * t1;
            float dist2 = startDistance + edgeLength * t2;
            float progress1 = dist1 / totalPerimeter;
            float progress2 = dist2 / totalPerimeter;
            int color1 = getBorderColorAtProgress(progress1, gradient, fallbackColor, time);
            int color2 = getBorderColorAtProgress(progress2, gradient, fallbackColor, time);
            addColoredVertex(builder, matrix, px1, y1, color1);
            addColoredVertex(builder, matrix, px1, y1 - borderWidth, color1);
            addColoredVertex(builder, matrix, px2, y2 - borderWidth, color2);
            addColoredVertex(builder, matrix, px1, y1, color1);
            addColoredVertex(builder, matrix, px2, y2 - borderWidth, color2);
            addColoredVertex(builder, matrix, px2, y2, color2);
        }
    }

    private static void addLeftBorderSegmented(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2,
                                                float borderWidth, int segments, float startDistance, float edgeLength, float totalPerimeter,
                                                GradientConfig gradient, Vector4i fallbackColor, long time) {
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / (float) segments;
            float t2 = (float) (i + 1) / (float) segments;
            float py1 = y1 + (y2 - y1) * t1;
            float py2 = y1 + (y2 - y1) * t2;
            float dist1 = startDistance + edgeLength * t1;
            float dist2 = startDistance + edgeLength * t2;
            float progress1 = dist1 / totalPerimeter;
            float progress2 = dist2 / totalPerimeter;
            int color1 = getBorderColorAtProgress(progress1, gradient, fallbackColor, time);
            int color2 = getBorderColorAtProgress(progress2, gradient, fallbackColor, time);
            addColoredVertex(builder, matrix, x1, py1, color1);
            addColoredVertex(builder, matrix, x1 + borderWidth, py1, color1);
            addColoredVertex(builder, matrix, x2 + borderWidth, py2, color2);
            addColoredVertex(builder, matrix, x1, py1, color1);
            addColoredVertex(builder, matrix, x2 + borderWidth, py2, color2);
            addColoredVertex(builder, matrix, x2, py2, color2);
        }
    }

    private static void addRoundedCornerBorderCircular(BufferBuilder builder, Matrix4f matrix, float centerX, float centerY,
                                                        float outerRadius, float innerRadius, float startAngle, float endAngle, int segments,
                                                        float startDistance, float arcLength, float totalPerimeter,
                                                        GradientConfig gradient, Vector4i fallbackColor, long time) {
        float angleStep = (endAngle - startAngle) / (float) segments;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(startAngle + angleStep * (float) i);
            float angle2 = (float) Math.toRadians(startAngle + angleStep * (float) (i + 1));
            float x1Outer = centerX + (float) Math.cos(angle1) * outerRadius;
            float y1Outer = centerY + (float) Math.sin(angle1) * outerRadius;
            float x2Outer = centerX + (float) Math.cos(angle2) * outerRadius;
            float y2Outer = centerY + (float) Math.sin(angle2) * outerRadius;
            float x1Inner = centerX + (float) Math.cos(angle1) * innerRadius;
            float y1Inner = centerY + (float) Math.sin(angle1) * innerRadius;
            float x2Inner = centerX + (float) Math.cos(angle2) * innerRadius;
            float y2Inner = centerY + (float) Math.sin(angle2) * innerRadius;
            float t1 = (float) i / (float) segments;
            float t2 = (float) (i + 1) / (float) segments;
            float dist1 = startDistance + arcLength * t1;
            float dist2 = startDistance + arcLength * t2;
            float progress1 = dist1 / totalPerimeter;
            float progress2 = dist2 / totalPerimeter;
            int color1 = getBorderColorAtProgress(progress1, gradient, fallbackColor, time);
            int color2 = getBorderColorAtProgress(progress2, gradient, fallbackColor, time);
            addColoredVertex(builder, matrix, x1Outer, y1Outer, color1);
            addColoredVertex(builder, matrix, x1Inner, y1Inner, color1);
            addColoredVertex(builder, matrix, x2Inner, y2Inner, color2);
            addColoredVertex(builder, matrix, x1Outer, y1Outer, color1);
            addColoredVertex(builder, matrix, x2Inner, y2Inner, color2);
            addColoredVertex(builder, matrix, x2Outer, y2Outer, color2);
        }
    }

    private static int getColorAtPosition(float px, float py, float rectX, float rectY, float rectWidth, float rectHeight,
                                           GradientConfig gradient, Vector4i fallbackColor, long time) {
        if (gradient == null) {
            return fallbackColor.w << 24 | fallbackColor.x << 16 | fallbackColor.y << 8 | fallbackColor.z;
        }
        float progress = 0.0f;
        switch (gradient.getType()) {
            case HORIZONTAL:
            case ANIMATED:
                progress = (px - rectX) / rectWidth;
                break;
            case VERTICAL:
                progress = (py - rectY) / rectHeight;
                break;
            case RADIAL: {
                float centerX = rectX + rectWidth / 2.0f;
                float centerY = rectY + rectHeight / 2.0f;
                float dx = px - centerX;
                float dy = py - centerY;
                float maxDist = (float) Math.sqrt(rectWidth * rectWidth + rectHeight * rectHeight) / 2.0f;
                progress = (float) Math.sqrt(dx * dx + dy * dy) / maxDist;
                break;
            }
            case BORDER_CIRCULAR:
                progress = (px - rectX) / rectWidth;
                break;
        }
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        return gradient.getColorAt(progress, time);
    }

    private static int getBorderColorAtProgress(float progress, GradientConfig gradient, Vector4i fallbackColor, long time) {
        if (gradient == null || gradient.getType() != GradientConfig.GradientType.BORDER_CIRCULAR) {
            return fallbackColor.w << 24 | fallbackColor.x << 16 | fallbackColor.y << 8 | fallbackColor.z;
        }
        return gradient.getColorAt(progress, time);
    }

    private static void addColoredVertex(BufferBuilder builder, Matrix4f matrix, float x, float y, int color) {
        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        builder.vertex(matrix, x, y, 0.0f).color(r, g, b, a).endVertex();
    }
}
