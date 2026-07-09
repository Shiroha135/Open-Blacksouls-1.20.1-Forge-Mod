package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSetCovenant;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuiCovenant extends Screen {

    private static final ResourceLocation TEX_NODEN_FACE = new ResourceLocation(BlackSouls.MODID, "textures/gui/covenant/noden_face.png");
    private static final ResourceLocation TEX_TWEEDLE_FACE = new ResourceLocation(BlackSouls.MODID, "textures/gui/covenant/tweedle_face.png");
    private static final ResourceLocation TEX_NODEN_SPRITE = new ResourceLocation(BlackSouls.MODID, "textures/gui/covenant/noden_sprite.png");
    private static final ResourceLocation TEX_TWEEDLE_SPRITE = new ResourceLocation(BlackSouls.MODID, "textures/gui/covenant/tweedle_sprite.png");
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 230;
    private static final int TOP_H = 130;
    private static final int BOT_LEFT_W = 200;
    private static final int BOT_RIGHT_W = 120;

    private static final String[] STAT_KEYS = {
            "gui.blacksouls.stat.max_hp", "gui.blacksouls.stat.max_mp", "gui.blacksouls.stat.atk",
            "gui.blacksouls.stat.def", "gui.blacksouls.stat.matk", "gui.blacksouls.stat.mdef", "gui.blacksouls.stat.luc"
    };

    private int guiLeft;
    private int guiTop;
    private String hoveredCovenant = "";

    public GuiCovenant() {
        super(Component.literal("Covenant"));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        if (this.minecraft == null || this.minecraft.player == null) return;

        BSPlayerStats stats = this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats == null) return;

        hoveredCovenant = "";

        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, GUI_WIDTH, TOP_H);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop + TOP_H, BOT_LEFT_W, GUI_HEIGHT - TOP_H);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft + BOT_LEFT_W, guiTop + TOP_H, BOT_RIGHT_W, GUI_HEIGHT - TOP_H);

        int statX = guiLeft + 15;
        int valX = guiLeft + 80;
        int statY = guiTop + 10;

        guiGraphics.drawString(this.font, this.minecraft.player.getName().getString(), statX, statY, 0xFFFFFF, false);

        int labelColor = 0x5555FF;
        int valColor = 0xFFFFFF;

        String[] values = {
                String.valueOf((int)stats.hp), String.valueOf((int)stats.maxMp), String.valueOf((int)stats.attack),
                String.valueOf((int)stats.defense), String.valueOf((int)stats.magicAttack), String.valueOf((int)stats.magicDefense), String.valueOf((int)stats.luck)
        };

        for (int i = 0; i < STAT_KEYS.length; i++) {
            guiGraphics.drawString(this.font, I18n.get(STAT_KEYS[i]), statX, statY + 16 + i * 14, labelColor, false);
            guiGraphics.drawString(this.font, values[i], valX, statY + 16 + i * 14, valColor, false);
        }

        int rightTopX = guiLeft + BOT_LEFT_W + 10;
        guiGraphics.drawString(this.font, I18n.get("gui.blacksouls.covenant.title"), rightTopX, guiTop + 10, labelColor, false);

        if (!stats.activeCovenant.isEmpty()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int origW = "tweedle".equals(stats.activeCovenant) ? 47 : 32;
            int renderH = 20;
            int renderW = origW * renderH / 32;
            guiGraphics.blit(getSpriteFor(stats.activeCovenant), rightTopX, guiTop + 25, renderW, renderH, 0, 0, origW, 32, origW, 32);
            guiGraphics.drawString(this.font, I18n.get("gui.blacksouls.covenant.prefix", getNameFor(stats.activeCovenant)), rightTopX + renderW + 5, guiTop + 31, valColor, false);
        }

        int listX = guiLeft + BOT_LEFT_W + 10;
        int listY = guiTop + TOP_H + 10;
        for (String covId : stats.unlockedCovenants) {
            if (!covId.equals(stats.activeCovenant)) {
                drawCovenantEntry(guiGraphics, covId, listX, listY, mouseX, mouseY);
                listY += 34;
            }
        }

        String displayCov = hoveredCovenant.isEmpty() ? stats.activeCovenant : hoveredCovenant;
        if (!displayCov.isEmpty()) {
            int detailY = guiTop + TOP_H + 10;
            guiGraphics.drawString(this.font, I18n.get("gui.blacksouls.covenant.prefix", getNameFor(displayCov)), guiLeft + 15, detailY, valColor, false);

            guiGraphics.drawString(this.font, getLevelFor(displayCov, stats), guiLeft + 15, detailY + 22, valColor, false);

            int textWrapWidth = BOT_LEFT_W - 15 - 50;
            String descText = getDescFor(displayCov, stats);
            List<FormattedCharSequence> splitDesc = this.font.split(Component.literal(descText), textWrapWidth);
            for (int i = 0; i < splitDesc.size(); i++) {
                guiGraphics.drawString(this.font, splitDesc.get(i), guiLeft + 15, detailY + 38 + i * 12, valColor, false);
            }

            int targetSize = 35;
            guiGraphics.blit(getFaceFor(displayCov), guiLeft + BOT_LEFT_W - targetSize - 10, detailY + 2, targetSize, targetSize, 0, 0, 96, 96, 96, 96);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawCovenantEntry(GuiGraphics guiGraphics, String id, int x, int y, int mouseX, int mouseY) {
        int entryWidth = BOT_RIGHT_W - 20;
        boolean isHovered = mouseX >= x - 5 && mouseX <= x + entryWidth && mouseY >= y - 2 && mouseY <= y + 26;
        if (isHovered) {
            hoveredCovenant = id;
            guiGraphics.fill(x - 5, y - 2, x + entryWidth, y + 26, 0x44FFFFFF);
        }
        int origW = "tweedle".equals(id) ? 47 : 32;
        int renderW = origW * 20 / 32;
        guiGraphics.blit(getSpriteFor(id), x, y + 2, renderW, 20, 0, 0, origW, 32, origW, 32);
        guiGraphics.drawString(this.font, I18n.get("gui.blacksouls.covenant.prefix", getNameFor(id)), x + renderW + 5, y + 8, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && !hoveredCovenant.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
            BSPlayerStats stats = this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
            if (stats != null && !hoveredCovenant.equals(stats.activeCovenant)) {
                stats.activeCovenant = hoveredCovenant;
                NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(), new PacketSetCovenant(hoveredCovenant, -1));
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE || this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.minecraft.setScreen(new GuiPlayerStats());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private ResourceLocation getFaceFor(String id) { return "tweedle".equals(id) ? TEX_TWEEDLE_FACE : TEX_NODEN_FACE; }
    private ResourceLocation getSpriteFor(String id) { return "tweedle".equals(id) ? TEX_TWEEDLE_SPRITE : TEX_NODEN_SPRITE; }

    private String getNameFor(String id) {
        String key = "covenant.blacksouls." + id + ".name";
        return I18n.exists(key) ? I18n.get(key) : I18n.get("covenant.blacksouls.unknown.name");
    }

    private String getLevelFor(String id, BSPlayerStats stats) {
        return I18n.get("gui.blacksouls.covenant.level") + ("noden".equals(id) ? " Lv." + stats.nodenCovenantLevel : "");
    }

    private String getDescFor(String id, BSPlayerStats stats) {
        if ("noden".equals(id)) {
            int bonus = stats.nodenCovenantLevel == 1 ? 25 : stats.nodenCovenantLevel == 2 ? 35 : stats.nodenCovenantLevel >= 3 ? 50 : 10;
            return I18n.get("covenant.blacksouls.noden.desc", bonus);
        }
        String key = "covenant.blacksouls." + id + ".desc";
        return I18n.exists(key) ? I18n.get(key) : "";
    }
}
