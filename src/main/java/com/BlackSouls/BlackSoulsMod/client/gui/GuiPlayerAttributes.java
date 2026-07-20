package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.client.ClientSkillInfo;
import com.BlackSouls.BlackSoulsMod.client.render.BSAvatarRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.platform.InputConstants;

public class GuiPlayerAttributes extends Screen {

    private static final int GUI_WIDTH = 280;
    private static final Component[] LEFT_LABELS = {
            Component.translatable("gui.blacksouls.stat.atk"),
            Component.translatable("gui.blacksouls.stat.def"),
            Component.translatable("gui.blacksouls.stat.matk"),
            Component.translatable("gui.blacksouls.stat.mdef"),
            Component.translatable("gui.blacksouls.stat.speed"),
            Component.translatable("gui.blacksouls.stat.luc")
    };
    private static final Component[] RIGHT_LABELS = {
            Component.translatable("gui.blacksouls.stat.hit_rate"),
            Component.translatable("gui.blacksouls.stat.evasion"),
            Component.translatable("gui.blacksouls.stat.crit"),
            Component.translatable("gui.blacksouls.stat.meva"),
            Component.translatable("gui.blacksouls.stat.counter"),
            Component.translatable("gui.blacksouls.stat.hp_regen"),
            Component.translatable("gui.blacksouls.stat.mp_regen"),
            Component.translatable("gui.blacksouls.stat.def_effect"),
            Component.translatable("gui.blacksouls.stat.mp_cost_rate")
    };
    private final int[] cachedLeftValues = new int[LEFT_LABELS.length];
    private final String[] leftValues = new String[LEFT_LABELS.length];
    private final String[] rightValues = {"100%", null, null, "0%", "0%", "0%", null, "250%", "100%"};
    private int cachedEvasion = Integer.MIN_VALUE;
    private int cachedCritRate = Integer.MIN_VALUE;
    private double cachedMpRegenRate = Double.NaN;

    public GuiPlayerAttributes() {
        super(Component.translatable("gui.blacksouls.menu.attributes"));
        java.util.Arrays.fill(this.cachedLeftValues, Integer.MIN_VALUE);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        if (this.minecraft == null || this.minecraft.player == null) return;
        Player player = this.minecraft.player;
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats == null) return;
        int guiLeft = this.width - GUI_WIDTH;
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, 0, GUI_WIDTH, this.height);
        int contentHeight = 270; 
        int offsetY = Math.max(0, (this.height - contentHeight) / 2);
        int labelColor = 0x5555FF;
        int valColor = 0xFFFFFF;
        int topY = offsetY + 15;
        String playerName = player.getName().getString();
        guiGraphics.drawString(font, playerName, guiLeft + 15, topY, valColor, false);
        int lvX = guiLeft + 130;
        guiGraphics.drawString(font, "Lv", lvX, topY, labelColor, false);
        guiGraphics.drawString(font, String.valueOf(stats.level), lvX + 20, topY, valColor, false);
        int avatarY = topY + 15;
        String avatarName = ClientSkillInfo.getAvatar();
        if (avatarName == null) avatarName = "default";
        var currentAvatarTex = BSAvatarRenderer.getTexture(avatarName);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BSAvatarRenderer.draw(guiGraphics, currentAvatarTex, avatarName, guiLeft + 15, avatarY, 60);
        RenderSystem.disableBlend();
        int rightTextX = guiLeft + 85;
        guiGraphics.drawString(font, I18n.get("gui.blacksouls.title.undead"), rightTextX, avatarY + 5, valColor, false);
        int barW = 130;
        int barH = 5;
        int barYOffset = 4;
        int hpY = avatarY + 25;
        float effectiveMaxHp = player.getMaxHealth();
        double hpP = Math.max(0.0, Math.min(1.0, player.getHealth() / Math.max(1.0f, effectiveMaxHp)));
        guiGraphics.fill(rightTextX, hpY + barYOffset, rightTextX + barW, hpY + barYOffset + barH, 0xFF440000);
        guiGraphics.fill(rightTextX, hpY + barYOffset, rightTextX + (int)(barW * hpP), hpY + barYOffset + barH, 0xFFFF3333);
        guiGraphics.drawString(font, "HP", rightTextX + 2, hpY, labelColor, true);
        String hpTxt = (int)player.getHealth() + " / " + (int)effectiveMaxHp;
        guiGraphics.drawString(font, hpTxt, rightTextX + barW - font.width(hpTxt) - 2, hpY, valColor, true);
        int mpY = avatarY + 40;
        double mpP = Math.max(0.0, Math.min(1.0, stats.mp / Math.max(1.0, stats.maxMp)));
        guiGraphics.fill(rightTextX, mpY + barYOffset, rightTextX + barW, mpY + barYOffset + barH, 0xFF000044);
        guiGraphics.fill(rightTextX, mpY + barYOffset, rightTextX + (int)(barW * mpP), mpY + barYOffset + barH, 0xFF3333FF);
        guiGraphics.drawString(font, "MP", rightTextX + 2, mpY, labelColor, true);
        String mpTxt = (int)stats.mp + " / " + (int)stats.maxMp;
        guiGraphics.drawString(font, mpTxt, rightTextX + barW - font.width(mpTxt) - 2, mpY, valColor, true);
        guiGraphics.fill(guiLeft + 10, avatarY + 65, guiLeft + GUI_WIDTH - 10, avatarY + 66, 0x33FFFFFF);
        int expY = avatarY + 72;
        int rightEdge = guiLeft + GUI_WIDTH - 15; 
        guiGraphics.drawString(font, I18n.get("gui.blacksouls.stat.current_exp"), guiLeft + 15, expY, labelColor, false);
        String curExpStr = String.valueOf(stats.currentExp);
        guiGraphics.drawString(font, curExpStr, rightEdge - font.width(curExpStr), expY, valColor, false);
        guiGraphics.drawString(font, I18n.get("gui.blacksouls.stat.next_exp"), guiLeft + 15, expY + 16, labelColor, false);
        String nextExpStr = stats.level >= 999 ? "---" : String.valueOf(Math.max(0, stats.maxExp - stats.currentExp));
        guiGraphics.drawString(font, nextExpStr, rightEdge - font.width(nextExpStr), expY + 16, valColor, false);
        guiGraphics.fill(guiLeft + 10, expY + 34, guiLeft + GUI_WIDTH - 10, expY + 35, 0x33FFFFFF);
        int statY = expY + 42;
        int leftColX = guiLeft + 15;
        int rightColX = guiLeft + 135;
        int leftValEdge = guiLeft + 120;
        int lineHeight = 14;

        updateLeftValue(0, (int) stats.attack);
        updateLeftValue(1, (int) stats.defense);
        updateLeftValue(2, (int) stats.magicAttack);
        updateLeftValue(3, (int) stats.magicDefense);
        updateLeftValue(4, (int) stats.speed);
        updateLeftValue(5, (int) stats.luck);
        int evasion = (int) stats.evasion;
        if (this.rightValues[1] == null || evasion != this.cachedEvasion) {
            this.cachedEvasion = evasion;
            this.rightValues[1] = evasion + "%";
        }
        int critRate = (int) stats.critRate;
        if (this.rightValues[2] == null || critRate != this.cachedCritRate) {
            this.cachedCritRate = critRate;
            this.rightValues[2] = critRate + "%";
        }
        if (this.rightValues[6] == null || Double.compare(stats.mpRegenRate, this.cachedMpRegenRate) != 0) {
            this.cachedMpRegenRate = stats.mpRegenRate;
            this.rightValues[6] = String.format("%.1f", stats.mpRegenRate * 100) + "%";
        }

        for (int i = 0; i < LEFT_LABELS.length; i++) {
            int y = statY + i * lineHeight;
            guiGraphics.drawString(font, LEFT_LABELS[i], leftColX, y, labelColor, false);
            guiGraphics.drawString(font, this.leftValues[i], leftValEdge - font.width(this.leftValues[i]), y, valColor, false);
        }

        for (int i = 0; i < RIGHT_LABELS.length; i++) {
            int y = statY + i * lineHeight;
            guiGraphics.drawString(font, RIGHT_LABELS[i], rightColX, y, labelColor, false);
            guiGraphics.drawString(font, this.rightValues[i], rightEdge - font.width(this.rightValues[i]), y, valColor, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && (keyCode == InputConstants.KEY_ESCAPE || this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.minecraft.setScreen(new GuiPlayerStats());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void updateLeftValue(int index, int value) {
        if (this.leftValues[index] == null || this.cachedLeftValues[index] != value) {
            this.cachedLeftValues[index] = value;
            this.leftValues[index] = String.valueOf(value);
        }
    }
}
