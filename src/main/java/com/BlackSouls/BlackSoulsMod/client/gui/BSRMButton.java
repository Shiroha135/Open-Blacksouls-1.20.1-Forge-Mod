package com.BlackSouls.BlackSoulsMod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class BSRMButton extends Button {

    public BSRMButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }
    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            int x = this.getX();
            int y = this.getY();
            BSGuiUtils.drawRMWindow(guiGraphics, x, y, this.width, this.height);
            if (this.isHoveredOrFocused()) {
                guiGraphics.fill(x + 2, y + 2, x + this.width - 2, y + this.height - 2, 0x44FFFFFF);
            }
            int textColor = 14737632;
            if (!this.active) {
                textColor = 10526880;
            } else if (this.isHoveredOrFocused()) {
                textColor = 16777120;
            }
            Font font = Minecraft.getInstance().font;
            int textY = y + (this.height - font.lineHeight) / 2;
            guiGraphics.drawCenteredString(font, this.getMessage(), x + this.width / 2, textY, textColor);
        }
    }
}