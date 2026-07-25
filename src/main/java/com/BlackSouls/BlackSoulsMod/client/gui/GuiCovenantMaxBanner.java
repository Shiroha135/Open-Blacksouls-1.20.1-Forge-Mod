package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.client.render.FadedBannerRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiCovenantMaxBanner extends Screen {

    private int timeLeft;

    public GuiCovenantMaxBanner() {
        super(Component.literal(""));
        this.timeLeft = 60; 
    }

    @Override
    public void tick() {
        super.tick();
        if (timeLeft > 0) {
            timeLeft--;
        } else {
            this.onClose(); 
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int MAIN_BOX_H = 90; 
        int mainBoxTop = this.height - MAIN_BOX_H;
        FadedBannerRenderer.draw(guiGraphics, 0, mainBoxTop, this.width, this.height);
        String line1 = I18n.get("gui.blacksouls.covenant.banner.max_1");
        String line2 = I18n.get("gui.blacksouls.covenant.banner.max_2");

        int textX = 20; 
        int textY = mainBoxTop + 25; 

        guiGraphics.drawString(font, line1, textX, textY, 0xFFFFFF, false);
        guiGraphics.drawString(font, line2, textX, textY + font.lineHeight + 5, 0xFFFFFF, false);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
