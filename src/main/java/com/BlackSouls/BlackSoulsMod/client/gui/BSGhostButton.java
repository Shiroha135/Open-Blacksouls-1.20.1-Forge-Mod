package com.BlackSouls.BlackSoulsMod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class BSGhostButton extends Button {

    public BSGhostButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = this.getX();
        int y = this.getY();
        if (this.isHoveredOrFocused()) {
            int fillColor = this.active ? 0x33FFFFFF : 0x22111111;
            int borderColor = this.active ? 0xAAFFFFFF : 0x55777777;
            guiGraphics.fill(x, y, x + this.width, y + this.height, fillColor);
            guiGraphics.fill(x, y, x + this.width, y + 1, borderColor);
            guiGraphics.fill(x, y + this.height - 1, x + this.width, y + this.height, borderColor);
            guiGraphics.fill(x, y, x + 1, y + this.height, borderColor);
            guiGraphics.fill(x + this.width - 1, y, x + this.width, y + this.height, borderColor);
        }

        Font font = Minecraft.getInstance().font;
        int textColor = this.active ? (this.isHoveredOrFocused() ? 0xFFF8D86C : 0xFFE8E8E8) : 0xFF777777;
        guiGraphics.drawCenteredString(font, this.getMessage(), x + this.width / 2, y + (this.height - font.lineHeight) / 2, textColor);
    }
}
