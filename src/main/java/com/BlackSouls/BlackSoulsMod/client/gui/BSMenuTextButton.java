package com.BlackSouls.BlackSoulsMod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class BSMenuTextButton extends Button {

    public BSMenuTextButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }
    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = this.getX();
        int y = this.getY();
        if (this.isHoveredOrFocused()) {
            guiGraphics.fill(x, y, x + this.width, y + this.height, 0x66FFFFFF);
            guiGraphics.fill(x, y, x + this.width, y + 1, 0xAAFFFFFF); 
            guiGraphics.fill(x, y + this.height - 1, x + this.width, y + this.height, 0xAAFFFFFF); 
            guiGraphics.fill(x, y, x + 1, y + this.height, 0xAAFFFFFF); 
            guiGraphics.fill(x + this.width - 1, y, x + this.width, y + this.height, 0xAAFFFFFF); 
        }
        int textColor = this.active ? 14737632 : 10526880;
        float textScale = 2.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(textScale, textScale, 1.0F);
        Font font = Minecraft.getInstance().font;
        float scaledX = (x + this.width / 2.0F) / textScale;
        float scaledY = (y + (this.height - font.lineHeight * textScale) / 2.0F) / textScale;
        guiGraphics.drawCenteredString(font, this.getMessage(), (int) scaledX, (int) scaledY, textColor);
        guiGraphics.pose().popPose();
    }
}