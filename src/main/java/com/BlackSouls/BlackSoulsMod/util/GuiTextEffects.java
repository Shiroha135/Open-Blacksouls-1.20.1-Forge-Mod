package com.BlackSouls.BlackSoulsMod.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class GuiTextEffects {

    public static void drawJumpingText(GuiGraphics graphics, Font font, String text, int x, int y, long time) {
        int cursor = x;

        for (int i = 0; i < text.length(); i++) {
            String s = String.valueOf(text.charAt(i));

            int offsetY = (int) Math.round(Math.sin(time / 130.0D + i * 0.75D) * 2.0D);
            int color = titleColor(i, time);

            graphics.drawString(font, s, cursor, y + offsetY, color, false);
            cursor += font.width(s);
        }
    }

    private static int titleColor(int index, long time) {
        float t = (float) ((Math.sin(time / 260.0D + index * 0.55D) + 1.0D) * 0.5D);

        int r = lerp(210, 255, t);
        int g = lerp(150, 230, t);
        int b = 255;

        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int lerp(int a, int b, float t) {
        return a + Math.round((b - a) * t);
    }
}