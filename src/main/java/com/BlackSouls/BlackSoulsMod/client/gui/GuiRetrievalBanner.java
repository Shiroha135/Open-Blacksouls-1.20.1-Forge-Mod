package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.client.render.FadedBannerRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class GuiRetrievalBanner extends Screen {

    private int timeLeft = 60;

    private static final float TEXT_SCALE = 3.5F;

    public GuiRetrievalBanner() {
        super(Component.translatable("gui.blacksouls.death.retrieval"));
    }

    @Override
    public void tick() {
        super.tick();
        if (timeLeft > 0) {
            timeLeft--;
        } else if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int screenWidth = this.width;
        int screenHeight = this.height;

        int bannerHeight = (int)(screenHeight * 0.15F);
        int y1 = (int)(screenHeight * 0.33F);
        int y2 = y1 + bannerHeight;

        FadedBannerRenderer.draw(guiGraphics, 0, y1, screenWidth, y2);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);

        int textX = (int) (screenWidth / 2.0F / TEXT_SCALE);
        int textY = (int) ((y1 + (bannerHeight - this.font.lineHeight * TEXT_SCALE) / 2.0F) / TEXT_SCALE);

        int titleWidth = this.font.width(this.title);
        guiGraphics.drawString(this.font, this.title, textX - titleWidth / 2, textY, 0x55FF55, false);
        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
