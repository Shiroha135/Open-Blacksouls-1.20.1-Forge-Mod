package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.client.ClientSkillInfo;
import com.BlackSouls.BlackSoulsMod.client.render.BSAvatarRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.platform.InputConstants;

public class GuiPlayerStats extends Screen {

    private static final int GUI_WIDTH = 370;
    private static final int GUI_HEIGHT = 250;
    private static final String UNKNOWN_BIOME = "???";

    private static final Component[] MENU_LABELS = {
            Component.translatable("gui.blacksouls.menu.skills"),
            Component.translatable("gui.blacksouls.menu.covenants"),
            Component.translatable("gui.blacksouls.menu.attributes"),
            Component.translatable("gui.blacksouls.menu.game_end")
    };

    private int guiLeft;
    private int guiTop;

    public GuiPlayerStats() {
        super(Component.translatable("gui.blacksouls.stats.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        this.clearWidgets();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        if (this.minecraft == null || this.minecraft.player == null) return;
        Player player = this.minecraft.player;

        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats == null) return;

        int menuW = 90, menuH = 125;
        int senW = 90, senH = 40;
        int soulW = 90, soulH = 45;
        int mainW = 280, mainH = 250;
        int locW = 120, locH = 50;

        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, menuW, menuH);
        int soulY = guiTop + mainH - soulH;
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, soulY, soulW, soulH);
        int senY = soulY - senH + 8;
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, senY, senW, senH);
        int mainX = guiLeft + menuW;
        BSGuiUtils.drawRMWindow(guiGraphics, mainX, guiTop, mainW, mainH);
        int locX = guiLeft + menuW + mainW - locW;
        int locY = guiTop + mainH - locH;
        BSGuiUtils.drawRMWindow(guiGraphics, locX, locY, locW, locH);

        int boxStartX = guiLeft + 8;
        int boxEndX = guiLeft + menuW - 8;
        for (int i = 0; i < 4; i++) {
            int mX = guiLeft + 12;
            int mY = guiTop + 15 + i * 26;
            boolean isHovered = mouseX >= boxStartX && mouseX <= boxEndX && mouseY >= mY - 4 && mouseY <= mY + 12;
            Component menuText = MENU_LABELS[i];

            if (isHovered) {
                guiGraphics.fill(boxStartX, mY - 4, boxEndX, mY + 12, 0x66FFFFFF);
                guiGraphics.drawString(font, menuText, mX, mY, 0xFFFFFF, false);
            } else {
                guiGraphics.drawString(font, menuText, mX, mY, 0xAAAAAA, false);
            }
        }

        int tagColor = 0x5555FF; 
        guiGraphics.drawString(font, I18n.get("gui.blacksouls.stat.sen"), guiLeft + 12, senY + 10, tagColor, false);

        String senText = String.valueOf(stats.sen);
        guiGraphics.drawString(font, senText, guiLeft + senW - 12 - font.width(senText), senY + 20, 0xFFFFFF, false);

        String soulNumText = String.valueOf(stats.souls);
        String soulSymbolText = " S";
        int totalSoulWidth = font.width(soulNumText) + font.width(soulSymbolText);
        int soulStartX = guiLeft + soulW - 12 - totalSoulWidth;

        guiGraphics.drawString(font, soulNumText, soulStartX, soulY + 20, 0xFFFFFF, false); 
        guiGraphics.drawString(font, soulSymbolText, soulStartX + font.width(soulNumText), soulY + 20, tagColor, false);

        guiGraphics.drawString(font, I18n.get("gui.blacksouls.stat.location"), locX + 12, locY + 10, tagColor, false);
        guiGraphics.drawString(font, getBiomeName(player), locX + 12, locY + 28, 0xFFFFFF, false);

        int mainY = guiTop;
        int avatarX = mainX + 12, avatarY = mainY + 12, avatarSize = 60;
        String avatarName = ClientSkillInfo.getAvatar();
        if (avatarName == null) avatarName = "default";
        ResourceLocation currentAvatarTex = BSAvatarRenderer.getTexture(avatarName);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BSAvatarRenderer.draw(guiGraphics, currentAvatarTex, avatarName, avatarX, avatarY, avatarSize);
        RenderSystem.disableBlend();

        int textX = mainX + 80;
        int textY = mainY + 12;
        String playerName = player.getName().getString();
        guiGraphics.drawString(font, playerName, textX, textY, 0xFFFFFF, false);
        guiGraphics.drawString(font, I18n.get("gui.blacksouls.title.undead"), textX + font.width(playerName) + 25, textY, 0xFFFFFF, false);

        guiGraphics.drawString(font, I18n.get("gui.blacksouls.stat.lv"), textX, textY + 20, tagColor, false);
        guiGraphics.drawString(font, String.valueOf(stats.level), textX + 20, textY + 20, 0xFFFFFF, false);

        int barStartX = textX + 45;
        int barW = 105;
        int barH = 5;
        int yOffset = 4;

        int hpY = textY + 35;
        float effectiveMaxHp = player.getMaxHealth();
        double hpP = Math.max(0.0, Math.min(1.0, player.getHealth() / Math.max(1.0f, effectiveMaxHp)));
        guiGraphics.fill(barStartX, hpY + yOffset, barStartX + barW, hpY + yOffset + barH, 0xFF440000);
        guiGraphics.fill(barStartX, hpY + yOffset, barStartX + (int)(barW * hpP), hpY + yOffset + barH, 0xFFFF3333);
        guiGraphics.drawString(font, I18n.get("gui.blacksouls.stat.hp"), barStartX + 2, hpY, tagColor, true);
        String hpTxt = (int)player.getHealth() + " / " + (int)effectiveMaxHp;
        guiGraphics.drawString(font, hpTxt, barStartX + barW - font.width(hpTxt) - 2, hpY, 0xFFFFFF, true);

        int mpY = textY + 50;
        double mpP = Math.max(0.0, Math.min(1.0, stats.mp / Math.max(1.0, stats.maxMp)));
        guiGraphics.fill(barStartX, mpY + yOffset, barStartX + barW, mpY + yOffset + barH, 0xFF000044);
        guiGraphics.fill(barStartX, mpY + yOffset, barStartX + (int)(barW * mpP), mpY + yOffset + barH, 0xFF3333FF);
        guiGraphics.drawString(font, I18n.get("gui.blacksouls.stat.mp"), barStartX + 2, mpY, tagColor, true);
        String mpTxt = (int)stats.mp + " / " + (int)stats.maxMp;
        guiGraphics.drawString(font, mpTxt, barStartX + barW - font.width(mpTxt) - 2, mpY, 0xFFFFFF, true);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @SuppressWarnings("resource")
    private String getBiomeName(Player player) {
        if (player == null) return UNKNOWN_BIOME;
        net.minecraft.world.level.Level level = player.level();
        BlockPos pos = player.blockPosition();
        ResourceLocation biomeKey = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(level.getBiome(pos).value());
        if (biomeKey != null) {
            String transKey = "biome." + biomeKey.getNamespace() + "." + biomeKey.getPath();
            if (I18n.exists(transKey)) return I18n.get(transKey);
            return biomeKey.getPath();
        }
        return UNKNOWN_BIOME;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null) {
            int mainX = guiLeft + 90, avatarX = mainX + 12, avatarY = guiTop + 12, avatarSize = 60;
            if (mouseX >= avatarX && mouseX <= avatarX + avatarSize && mouseY >= avatarY && mouseY <= avatarY + avatarSize) {
                if (BlackSouls.CURSOR1_EVENT != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
                this.minecraft.setScreen(new GuiAvatarSelect(this));
                return true;
            }
            int boxStartX = guiLeft + 8, boxEndX = guiLeft + 90 - 8;
            if (mouseX >= boxStartX && mouseX <= boxEndX) {
                boolean playedSound = false;

                if (mouseY >= guiTop + 11 && mouseY <= guiTop + 27) { 
                    playedSound = true;
                    this.minecraft.setScreen(new GuiAdvancedSkill());
                } else if (mouseY >= guiTop + 37 && mouseY <= guiTop + 53) { 
                    playedSound = true;
                    this.minecraft.setScreen(new GuiCovenant());
                } else if (mouseY >= guiTop + 63 && mouseY <= guiTop + 79) { 
                    playedSound = true;
                    this.minecraft.setScreen(new GuiPlayerAttributes());
                } else if (mouseY >= guiTop + 89 && mouseY <= guiTop + 105) {
                    playedSound = true;
                    this.onClose();
                }

                if (playedSound && BlackSouls.CURSOR1_EVENT != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && (keyCode == InputConstants.KEY_ESCAPE || this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
