package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.FadedBannerRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class GuiCovenantUpgradeBanner extends Screen {

    private static final ResourceLocation TEX_SOUL_ICON = new ResourceLocation(BlackSouls.MODID, "textures/item/consumable/soul_weak.png");

    private int timeLeft;
    private final String npcNameKey;
    private final String npcAvatarId;
    private final int entityId;
    private final int currentLevel;
    private final long cost;

    private static final int BANNER_TOP_H = 50;
    private static final int ICON_SIZE = 16;
    private static final int PADDING_LEFT = 20;

    public GuiCovenantUpgradeBanner(String npcNameKey, String npcAvatarId, int entityId, int currentLevel, long cost) {
        super(Component.literal(""));
        this.npcNameKey = npcNameKey;
        this.npcAvatarId = npcAvatarId;
        this.entityId = entityId;
        this.currentLevel = currentLevel;
        this.cost = cost;

        this.timeLeft = 60;
    }

    @Override
    public void tick() {
        super.tick();
        if (timeLeft > 0) {
            timeLeft--;
        } else if (this.minecraft != null) {
            String finalDialogueKey = "dialogue.blacksouls.noden.upgrade_success";
            this.minecraft.setScreen(new GuiDialogueEnhanced(this.npcNameKey, this.npcAvatarId, new String[]{finalDialogueKey}, false, this.entityId, this.currentLevel + 1));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) { 

        int topBannerY = (this.height - BANNER_TOP_H) / 2;

        FadedBannerRenderer.draw(guiGraphics, 0, topBannerY, this.width, topBannerY + BANNER_TOP_H);

        String textSuccess = I18n.get("gui.blacksouls.covenant.banner.upgrade_success");
        int textSuccessY = topBannerY + (BANNER_TOP_H - font.lineHeight) / 2;
        guiGraphics.drawString(font, textSuccess, PADDING_LEFT, textSuccessY, 0x00FFFFFF, false);

        int botBannerH = font.lineHeight + 8;
        int botBannerW = this.width;
        int botBannerX = 0;
        int botBannerY = this.height - botBannerH;

        FadedBannerRenderer.draw(guiGraphics, botBannerX, botBannerY, botBannerX + botBannerW, this.height);

        String textLostTitle = I18n.get("gui.blacksouls.dialogue.covenant.lost_souls");
        int textLostY = botBannerY - font.lineHeight - 5;
        guiGraphics.drawString(font, textLostTitle, PADDING_LEFT, textLostY, 0xFFFFFFFF, false);

        int lineCostCenterY = botBannerY + (botBannerH - font.lineHeight) / 2;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int drawIconX = PADDING_LEFT;
        int drawIconY = lineCostCenterY + (font.lineHeight - ICON_SIZE) / 2;
        guiGraphics.blit(TEX_SOUL_ICON, drawIconX, drawIconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();

        String textLostValue = "-" + cost + "s";
        int textValueX = drawIconX + ICON_SIZE + 5;

        guiGraphics.drawString(font, textLostValue, textValueX, lineCostCenterY, 0xFFFFFFFF, false);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
