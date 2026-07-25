package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.client.render.FadedBannerRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiCovenantFailBanner extends Screen {

    private int timeLeft;

    public GuiCovenantFailBanner() {
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
        String text = I18n.get("gui.blacksouls.covenant.banner.fail");
        int textX = 20;
        int textY = mainBoxTop + 25;
        guiGraphics.drawString(font, text, textX, textY, 0xFFFFFF, false);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
