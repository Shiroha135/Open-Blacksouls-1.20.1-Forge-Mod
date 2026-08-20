package com.BlackSouls.BlackSoulsMod.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ClientSpongeNameTooltipComponent implements ClientTooltipComponent {

    private final SpongeNameTooltipComponent component;

    public ClientSpongeNameTooltipComponent(SpongeNameTooltipComponent component) {
        this.component = component;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public int getWidth(Font font) {
        return font.width(component.text());
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
        String text = component.text().getString();
        float worldTime = Minecraft.getInstance().level != null
                ? (Minecraft.getInstance().level.getGameTime() + Minecraft.getInstance().getFrameTime())
                : (System.currentTimeMillis() % 100000L) / 35.0f;
        float flowTime = worldTime * 0.04f;
        renderGradientTitle(font, guiGraphics, text, x, y, flowTime, component.style());
    }

    private static void renderGradientTitle(Font font, GuiGraphics guiGraphics, String text, int x, int y, float flowTime, String style) {
        int drawX = x;
        int length = Math.max(1, text.length());
        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            float ratio = length == 1 ? 0.0f : (float) i / (float) (length - 1);
            float offset = normalizeCycle(ratio + flowTime);
            int color = sampleGradient(offset, style);
            guiGraphics.drawString(font, ch, drawX, y, color, false);
            drawX += font.width(ch);
        }
    }

    private static float normalizeCycle(float value) {
        return value - (float) Math.floor(value);
    }

    private static int sampleGradient(float offset, String style) {
        if ("rainbow".equals(style)) {
            if (offset < 0.1667f) return lerpColor(0xFF5050, 0xFFB347, offset / 0.1667f);
            if (offset < 0.3334f) return lerpColor(0xFFB347, 0xFFF45E, (offset - 0.1667f) / 0.1667f);
            if (offset < 0.5001f) return lerpColor(0xFFF45E, 0x7CFF72, (offset - 0.3334f) / 0.1667f);
            if (offset < 0.6668f) return lerpColor(0x7CFF72, 0x67C8FF, (offset - 0.5001f) / 0.1667f);
            if (offset < 0.8335f) return lerpColor(0x67C8FF, 0x9B74FF, (offset - 0.6668f) / 0.1667f);
            return lerpColor(0x9B74FF, 0xFF6CCF, (offset - 0.8335f) / 0.1665f);
        }
        if (offset < 0.5f) {
            return lerpColor(0xC86DFF, 0xF0B8FF, offset / 0.5f);
        }
        return lerpColor(0xF0B8FF, 0x8F72FF, (offset - 0.5f) / 0.5f);
    }

    private static int lerpColor(int from, int to, float alpha) {
        float t = Mth.clamp(alpha, 0.0f, 1.0f);
        int fromR = (from >> 16) & 0xFF;
        int fromG = (from >> 8) & 0xFF;
        int fromB = from & 0xFF;
        int toR = (to >> 16) & 0xFF;
        int toG = (to >> 8) & 0xFF;
        int toB = to & 0xFF;
        int resultR = Mth.floor(Mth.lerp(t, fromR, toR));
        int resultG = Mth.floor(Mth.lerp(t, fromG, toG));
        int resultB = Mth.floor(Mth.lerp(t, fromB, toB));
        return 0xFF000000 | (resultR << 16) | (resultG << 8) | resultB;
    }
}
