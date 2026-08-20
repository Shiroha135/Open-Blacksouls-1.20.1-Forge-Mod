package com.BlackSouls.BlackSoulsMod.client.render;

import java.util.List;

/**
 * 渐变色配置，思路取自 ArcaneVortex 的 GradientConfig，按实际用量精简。
 * 保存一组渐变颜色（ARGB）、动画速度、渐变类型与方向；核心是 {@link #getColorAt}，
 * 供圆角矩形渲染器逐顶点取色。
 */
public final class GradientConfig {
    private final List<Integer> colors;
    private final float speed;
    private final GradientType type;
    private final boolean clockwise;

    public GradientConfig(List<Integer> colors, float speed, GradientType type, boolean clockwise) {
        this.colors = colors;
        this.speed = speed;
        this.type = type;
        this.clockwise = clockwise;
    }

    public GradientType getType() {
        return type;
    }

    public int getColorAt(float progress, long time) {
        if (colors.isEmpty()) {
            return -1;
        }
        if (colors.size() == 1) {
            return colors.get(0);
        }
        float scaledProgress = scaledProgress(progress, time);
        int index1 = (int) scaledProgress % colors.size();
        int index2 = (index1 + 1) % colors.size();
        float blend = scaledProgress - (float) ((int) scaledProgress);
        return interpolateColor(colors.get(index1), colors.get(index2), blend);
    }

    private float scaledProgress(float progress, long time) {
        float offset = 0.0f;
        if (type == GradientType.ANIMATED || type == GradientType.BORDER_CIRCULAR) {
            offset = (float) (time % (long) (10000.0f / speed)) / (10000.0f / speed);
            if (!clockwise) {
                offset = -offset;
            }
        }
        float animatedProgress = (progress + offset) % 1.0f;
        if (animatedProgress < 0.0f) {
            animatedProgress += 1.0f;
        }
        return animatedProgress * (float) colors.size();
    }

    private static int interpolateColor(int color1, int color2, float factor) {
        int a1 = color1 >> 24 & 0xFF;
        int r1 = color1 >> 16 & 0xFF;
        int g1 = color1 >> 8 & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = color2 >> 24 & 0xFF;
        int r2 = color2 >> 16 & 0xFF;
        int g2 = color2 >> 8 & 0xFF;
        int b2 = color2 & 0xFF;
        if (a1 == 0) {
            a1 = 255;
        }
        if (a2 == 0) {
            a2 = 255;
        }
        int a = (int) ((float) a1 + (float) (a2 - a1) * factor);
        int r = (int) ((float) r1 + (float) (r2 - r1) * factor);
        int g = (int) ((float) g1 + (float) (g2 - g1) * factor);
        int b = (int) ((float) b1 + (float) (b2 - b1) * factor);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public enum GradientType {
        HORIZONTAL,
        VERTICAL,
        RADIAL,
        ANIMATED,
        BORDER_CIRCULAR
    }
}
