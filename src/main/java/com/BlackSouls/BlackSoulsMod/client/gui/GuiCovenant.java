package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.client.ClientStoryName;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSetCovenant;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundOriginalSpiritPacket;
import com.BlackSouls.BlackSoulsMod.util.OriginalSpiritData;
import java.util.ArrayList;
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

@SuppressWarnings("removal")
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
    private int covenantListOffset;
    private int covenantKeyboardIndex = -1;

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

        hoveredCovenant = covenantKeyboardIndex >= 0 && covenantKeyboardIndex < covenantEntries(stats).size()
                ? covenantEntries(stats).get(covenantKeyboardIndex) : "";

        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, GUI_WIDTH, TOP_H);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop + TOP_H, BOT_LEFT_W, GUI_HEIGHT - TOP_H);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft + BOT_LEFT_W, guiTop + TOP_H, BOT_RIGHT_W, GUI_HEIGHT - TOP_H);

        int statX = guiLeft + 15;
        int valX = guiLeft + 80;
        int statY = guiTop + 10;

        guiGraphics.drawString(this.font, ClientStoryName.get(this.minecraft.player), statX, statY, 0xFFFFFF, false);

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

        String activeKey = activeCovenantKey(stats);
        if (!activeKey.isEmpty()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int origW = "tweedle".equals(activeKey) ? 47 : 32;
            int renderH = 20;
            int renderW = origW * renderH / 32;
            if (getSpriteFor(activeKey) != null) guiGraphics.blit(getSpriteFor(activeKey), rightTopX, guiTop + 25, renderW, renderH, 0, 0, origW, 32, origW, 32);
            guiGraphics.drawString(this.font, I18n.get("gui.blacksouls.covenant.prefix", getNameFor(activeKey)), rightTopX + renderW + 5, guiTop + 31, valColor, false);
        }

        int listX = guiLeft + BOT_LEFT_W + 10;
        int listY = guiTop + TOP_H + 10;
        List<String> choices = covenantEntries(stats);
        int visible = Math.max(1, (GUI_HEIGHT - TOP_H - 20) / 30);
        covenantListOffset = Math.max(0, Math.min(covenantListOffset, Math.max(0, choices.size() - visible)));
        for (int index = covenantListOffset; index < Math.min(choices.size(), covenantListOffset + visible); index++) {
            drawCovenantEntry(guiGraphics, choices.get(index), listX, listY, mouseX, mouseY);
            listY += 30;
        }
        if (choices.size() > visible) guiGraphics.drawString(font, "↕", guiLeft + GUI_WIDTH - 12, guiTop + GUI_HEIGHT - 14, 0xAAAAAA, false);

        String displayCov = hoveredCovenant.isEmpty() ? activeKey : hoveredCovenant;
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
            if (getFaceFor(displayCov) != null) guiGraphics.blit(getFaceFor(displayCov), guiLeft + BOT_LEFT_W - targetSize - 10, detailY + 2, targetSize, targetSize, 0, 0, 96, 96, 96, 96);
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
        if (getSpriteFor(id) != null) guiGraphics.blit(getSpriteFor(id), x, y + 2, renderW, 20, 0, 0, origW, 32, origW, 32);
        guiGraphics.drawString(this.font, font.plainSubstrByWidth(getNameFor(id), entryWidth - renderW - 5), x + renderW + 5, y + 8, spiritId(id) > 4 ? 0x777777 : 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && !hoveredCovenant.isEmpty() && minecraft != null && minecraft.player != null) {
            var stats = minecraft.player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats != null && selectCovenant(hoveredCovenant, stats)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (minecraft == null || minecraft.player == null) return false;
        var stats = minecraft.player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) return false;
        int visible = Math.max(1, (GUI_HEIGHT - TOP_H - 20) / 30);
        covenantListOffset = Math.max(0, Math.min(Math.max(0, covenantEntries(stats).size() - visible), covenantListOffset + (delta < 0 ? 1 : -1)));
        covenantKeyboardIndex = -1;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.player != null) {
            var stats = minecraft.player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats != null) {
                List<String> entries = covenantEntries(stats);
                if (!entries.isEmpty() && (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UP || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN)) {
                    covenantKeyboardIndex = covenantKeyboardIndex < 0 ? 0 : Math.floorMod(covenantKeyboardIndex + (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN ? 1 : -1), entries.size());
                    int visible = Math.max(1, (GUI_HEIGHT - TOP_H - 20) / 30);
                    if (covenantKeyboardIndex < covenantListOffset) covenantListOffset = covenantKeyboardIndex;
                    if (covenantKeyboardIndex >= covenantListOffset + visible) covenantListOffset = covenantKeyboardIndex - visible + 1;
                    hoveredCovenant = entries.get(covenantKeyboardIndex);
                    return true;
                }
                if (covenantKeyboardIndex >= 0 && covenantKeyboardIndex < entries.size() && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) return selectCovenant(entries.get(covenantKeyboardIndex), stats);
            }
        }
        if (this.minecraft != null && (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE || this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.minecraft.setScreen(new GuiPlayerStats());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static int spiritId(String key) {
        return key.startsWith("spirit:") ? Integer.parseInt(key.substring(7)) : -1;
    }

    private static String activeCovenantKey(BSPlayerStats stats) {
        return stats.originalSpiritsInitialized && stats.originalSpiritPartner > 0 ? "spirit:" + stats.originalSpiritPartner : stats.activeCovenant;
    }

    private static List<String> covenantEntries(BSPlayerStats stats) {
        List<String> entries = new ArrayList<>();
        if (stats.originalSpiritsInitialized) {
            stats.originalSpirits.stream().sorted().forEach(id -> entries.add("spirit:" + id));
            stats.unlockedCovenants.stream().filter(id -> !id.equals("noden")).forEach(entries::add);
            entries.add("spirit:0");
        } else entries.addAll(stats.unlockedCovenants);
        return List.copyOf(entries);
    }

    private boolean selectCovenant(String key, BSPlayerStats stats) {
        int spirit = spiritId(key);
        if (spirit >= 0) {
            if (spirit > 0 && (!stats.originalSpirits.contains(spirit) || !OriginalSpiritData.supported(spirit))) return false;
            NetworkHandler.sendToServer(new ServerboundOriginalSpiritPacket(spirit));
            return true;
        }
        if (!stats.unlockedCovenants.contains(key) || key.equals(stats.activeCovenant)) return false;
        NetworkHandler.sendToServer(new PacketSetCovenant(key, -1));
        return true;
    }

    private ResourceLocation getFaceFor(String id) { return spiritId(id) == 0 || spiritId(id) > 4 ? null : "tweedle".equals(id) ? TEX_TWEEDLE_FACE : TEX_NODEN_FACE; }
    private ResourceLocation getSpriteFor(String id) { return spiritId(id) == 0 || spiritId(id) > 4 ? null : "tweedle".equals(id) ? TEX_TWEEDLE_SPRITE : TEX_NODEN_SPRITE; }

    private String getNameFor(String id) {
        int spirit = spiritId(id);
        if (spirit == 0) return I18n.get("gui.none");
        if (spirit > 0) return spirit <= 4 ? "诺登 Lv." + (spirit - 1) : OriginalSpiritData.get(spirit).name();
        String key = "covenant.blacksouls." + id + ".name";
        return I18n.exists(key) ? I18n.get(key) : I18n.get("covenant.blacksouls.unknown.name");
    }

    private String getLevelFor(String id, BSPlayerStats stats) {
        if (spiritId(id) >= 0) return spiritId(id) >= 1 && spiritId(id) <= 4 ? "Lv." + (spiritId(id) - 1) : "";
        return I18n.get("gui.blacksouls.covenant.level") + ("noden".equals(id) ? " Lv." + stats.nodenCovenantLevel : "");
    }

    private String getDescFor(String id, BSPlayerStats stats) {
        if (spiritId(id) >= 0) return spiritId(id) == 0 ? "" : OriginalSpiritData.get(spiritId(id)).description();
        if ("noden".equals(id)) {
            int bonus = stats.nodenCovenantLevel == 1 ? 25 : stats.nodenCovenantLevel == 2 ? 35 : stats.nodenCovenantLevel >= 3 ? 50 : 10;
            return I18n.get("covenant.blacksouls.noden.desc", bonus);
        }
        String key = "covenant.blacksouls." + id + ".desc";
        return I18n.exists(key) ? I18n.get(key) : "";
    }
}
