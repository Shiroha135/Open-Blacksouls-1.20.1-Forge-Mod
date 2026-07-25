package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.FadedBannerRenderer;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSetCovenant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class GuiCovenantUpgradeConfirm extends Screen {

    private final String npcNameKey;
    private final String npcAvatarId;
    private final int entityId;
    private final int currentLevel;
    private final long playerSouls;
    private final long cost;

    private static final int BANNER_H = 50;
    private static final int YN_W = 80;
    private static final int YN_H = 64;
    private static final int SOULS_W = 140;
    private static final int SOULS_H = 36;

    private int ynX, ynY;
    private int soulsX, soulsY;

    public GuiCovenantUpgradeConfirm(String npcNameKey, String npcAvatarId, int entityId, int currentLevel, long playerSouls, long cost) {
        super(Component.literal(""));
        this.npcNameKey = npcNameKey;
        this.npcAvatarId = npcAvatarId;
        this.entityId = entityId;
        this.currentLevel = currentLevel;
        this.playerSouls = playerSouls;
        this.cost = cost;
    }

    @Override
    protected void init() {
        super.init();
        int bannerY = this.height - BANNER_H;
        this.ynX = this.width - YN_W;
        this.ynY = bannerY - YN_H;
        this.soulsX = this.width - SOULS_W;
        this.soulsY = 0;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int bannerY = this.height - BANNER_H;
        FadedBannerRenderer.draw(guiGraphics, 0, bannerY, this.width, this.height);

        String textPart1 = I18n.get("gui.blacksouls.covenant.confirm.cost", cost);
        String textPart2 = I18n.get("gui.blacksouls.covenant.confirm.target");
        String textPart3 = I18n.get("gui.blacksouls.covenant.confirm.question");

        int textX = 40;
        int textY = bannerY + (BANNER_H - this.font.lineHeight) / 2;

        guiGraphics.drawString(this.font, textPart1, textX, textY, 0xFFFFFF, false);
        int offset1 = this.font.width(textPart1);
        guiGraphics.drawString(this.font, textPart2, textX + offset1, textY, 0xFFFF5555, false); 
        int offset2 = this.font.width(textPart2);
        guiGraphics.drawString(this.font, textPart3, textX + offset1 + offset2, textY, 0xFFFFFF, false);

        BSGuiUtils.drawRMWindow(guiGraphics, soulsX, soulsY, SOULS_W, SOULS_H);
        String soulsNum = String.valueOf(playerSouls);
        String sChar = " S";

        int totalW = this.font.width(soulsNum) + this.font.width(sChar);
        int drawSX = soulsX + (SOULS_W - totalW) / 2;
        int drawSY = soulsY + (SOULS_H - this.font.lineHeight) / 2;

        guiGraphics.drawString(this.font, soulsNum, drawSX, drawSY, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, sChar, drawSX + this.font.width(soulsNum), drawSY, 0x5555FF, false);

        BSGuiUtils.drawRMWindow(guiGraphics, ynX, ynY, YN_W, YN_H);

        int yesY = ynY + 14;
        int noY = ynY + 38;

        boolean hoverYes = mouseX >= ynX + 5 && mouseX <= ynX + YN_W - 5 && mouseY >= yesY - 4 && mouseY <= yesY + 12;
        boolean hoverNo = mouseX >= ynX + 5 && mouseX <= ynX + YN_W - 5 && mouseY >= noY - 4 && mouseY <= noY + 12;

        if (hoverYes) guiGraphics.fill(ynX + 8, yesY - 2, ynX + YN_W - 8, yesY + 12, 0x66FFFFFF);
        if (hoverNo) guiGraphics.fill(ynX + 8, noY - 2, ynX + YN_W - 8, noY + 12, 0x66FFFFFF);

        String yesText = I18n.get("gui.blacksouls.option.yes");
        String noText = I18n.get("gui.blacksouls.option.no");
        guiGraphics.drawCenteredString(this.font, yesText, ynX + YN_W / 2, yesY, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, noText, ynX + YN_W / 2, noY, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null) {
            int yesY = ynY + 14;
            int noY = ynY + 38;

            if (mouseX >= ynX + 5 && mouseX <= ynX + YN_W - 5) {
                if (mouseY >= yesY - 4 && mouseY <= yesY + 12) {
                    if (BlackSouls.CURSOR1_EVENT != null) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
                    }
                    if (playerSouls >= cost) {
                        NetworkHandler.sendToServer(new PacketSetCovenant("noden", currentLevel + 1));
                        this.minecraft.setScreen(new GuiCovenantUpgradeBanner(this.npcNameKey, this.npcAvatarId, this.entityId, this.currentLevel, this.cost));
                    } else {
                        this.minecraft.setScreen(new GuiCovenantFailBanner());
                    }
                    return true;
                }
                else if (mouseY >= noY - 4 && mouseY <= noY + 12) {
                    if (BlackSouls.CURSOR1_EVENT != null) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
                    }
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
