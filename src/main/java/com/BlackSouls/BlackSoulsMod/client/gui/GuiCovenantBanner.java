package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.client.render.FadedBannerRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n; 
import net.minecraft.network.chat.Component;

public class GuiCovenantBanner extends Screen {

    private final String npcName;
    private int tickCounter = 0;
    private static final String KEY_BANNER_TEXT = "gui.blacksouls.covenant.banner.text";

    public GuiCovenantBanner(String npcName) {
        super(Component.literal("Covenant Banner"));
        this.npcName = npcName;
    }

    @Override
    protected void init() {
        super.init();
        this.tickCounter = 0;
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;
        if (tickCounter > 60) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int bannerHeight = 60;
        int centerY = this.height / 2;
        int top = centerY - (bannerHeight / 2);
        int bottom = centerY + (bannerHeight / 2);
        FadedBannerRenderer.draw(guiGraphics, 0, top, this.width, bottom);
        String bannerText = I18n.get(KEY_BANNER_TEXT, npcName);
        int textX = (int) (this.width * 0.2f);
        guiGraphics.drawString(font, bannerText, textX, centerY - 4, 0xFFFFFF, true);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
