package com.BlackSouls.BlackSoulsMod.client.tooltip;

import com.BlackSouls.BlackSoulsMod.mixin.client.ClientTextTooltipAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.List;

public final class CustomTooltipRenderer {

    private CustomTooltipRenderer() {
    }

    public static void render(
            GuiGraphics graphics,
            Font font,
            ItemStack stack,
            List<ClientTooltipComponent> components,
            int mouseX,
            int mouseY,
            int screenWidth,
            int screenHeight,
            long time
    ) {
        if (components.isEmpty()) {
            return;
        }

        int contentWidth = Math.max(font.width(stack.getHoverName()), 40);
        int contentHeight = 12;

        for (int i = 1; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            contentWidth = Math.max(contentWidth, component.getWidth(font));
            contentHeight += component.getHeight();
        }

        int padding = 8;
        int boxWidth = contentWidth + padding * 2;
        int boxHeight = contentHeight + padding * 2 + 4;

        int x = mouseX + 18;
        int y = mouseY - boxHeight / 2;

        if (x + boxWidth > screenWidth - 6) {
            x = mouseX - boxWidth - 18;
        }
        if (y + boxHeight > screenHeight - 6) {
            y = screenHeight - boxHeight - 6;
        }
        if (x < 6) {
            x = 6;
        }
        if (y < 6) {
            y = 6;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 520.0F);

        drawBox(graphics, x, y, boxWidth, boxHeight, time);
        int textX = x + padding;
        int textY = y + padding;

        drawJumpingTitle(graphics, font, stack.getHoverName().getString(), textX, textY, time);

        textY += 12;
        drawSeparator(graphics, x + 6, textY, boxWidth - 12, time);
        textY += 4;

        Matrix4f matrix = graphics.pose().last().pose();
        MultiBufferSource.BufferSource buffer = graphics.bufferSource();

        for (int i = 1; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            if (component instanceof ClientTextTooltipAccessor textComponent) {
                font.drawInBatch(textComponent.blacksouls$getText(), textX, textY,
                        -1, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, 15728880);
            } else {
                component.renderText(font, textX, textY, matrix, buffer);
            }
            component.renderImage(font, textX, textY, graphics);
            textY += component.getHeight();
        }

        buffer.endBatch();
        graphics.pose().popPose();
    }

    private static void drawJumpingTitle(GuiGraphics graphics, Font font, String text, int x, int y, long time) {
        int offsetY = (int) Math.round(Math.sin(time / 180.0D));
        graphics.drawString(font, text, x, y + offsetY, titleColor(0, time), false);
    }

    private static void drawSeparator(GuiGraphics graphics, int x, int y, int width, long time) {
        for (int i = 0; i < width; i++) {
            float t = (i / (float) width + (time % 1800L) / 1800.0F) % 1.0F;
            graphics.fill(x + i, y, x + i + 1, y + 1, flowColor(t));
        }
        graphics.fill(x, y + 1, x + width, y + 2, 0x805C22AA);
    }

    private static void drawBox(GuiGraphics graphics, int x, int y, int width, int height, long time) {
        int shadow = 0xAA000000;
        int background = 0xE0100718;
        int inner = 0xFF12051F;
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, shadow);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, background);
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, inner);
        drawFlowBorder(graphics, x, y, width, height, time);
        graphics.fill(x + 6, y + 6, x + width - 6, y + 7, 0x80FFFFFF);
        graphics.fill(x + 6, y + height - 7, x + width - 6, y + height - 6, 0x806A20FF);
    }

    private static void drawFlowBorder(GuiGraphics graphics, int x, int y, int width, int height, long time) {
        int thickness = 3;
        int totalLength = Math.max(1, width * 2 + height * 2);
        int offset = (int) ((time % 2400L) / 2400.0F * totalLength);

        for (int i = 0; i < width; i++) {
            float t = ((i + offset) % totalLength) / (float) totalLength;
            graphics.fill(x + i, y, x + i + 1, y + thickness, flowColor(t));
        }

        for (int i = 0; i < height; i++) {
            float t = ((width + i + offset) % totalLength) / (float) totalLength;
            graphics.fill(x + width - thickness, y + i, x + width, y + i + 1, flowColor(t));
        }

        for (int i = 0; i < width; i++) {
            float t = ((width + height + (width - i) + offset) % totalLength) / (float) totalLength;
            graphics.fill(x + i, y + height - thickness, x + i + 1, y + height, flowColor(t));
        }

        for (int i = 0; i < height; i++) {
            float t = ((width * 2 + height + (height - i) + offset) % totalLength) / (float) totalLength;
            graphics.fill(x, y + i, x + thickness, y + i + 1, flowColor(t));
        }

        graphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xB0FFC8FF);
        graphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, 0xA06A20FF);
        graphics.fill(x + 3, y + 3, x + 4, y + height - 3, 0xA0FFC8FF);
        graphics.fill(x + width - 4, y + 3, x + width - 3, y + height - 3, 0xA06A20FF);
    }

    private static int flowColor(float t) {
        float wave = (float) Math.sin(t * Math.PI * 2.0D);
        int r = 180 + (int) (55 * wave);
        int g = 90 + (int) (55 * Math.sin(t * Math.PI * 2.0D + 2.1D));
        int b = 255;
        int a = 230;
        return a << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
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

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
