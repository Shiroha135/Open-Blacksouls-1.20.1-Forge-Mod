package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketDevSetStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"SameParameterValue", "unused"})
public class GuiDevPanel extends Screen {

    private EditBox fieldLevel, fieldHp, fieldMp, fieldAtk, fieldDef, fieldMAtk, fieldMDef, fieldLuck, fieldSpeed, fieldSouls, fieldSen;
    private static final int GUI_WIDTH = 340;
    private static final int GUI_HEIGHT = 220;
    private int guiLeft, guiTop;

    public GuiDevPanel() {
        super(Component.translatable("gui.blacksouls.dev_panel.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        this.clearWidgets();
        int col1X = guiLeft + 70;
        int col2X = guiLeft + 220;
        int startY = guiTop + 35;
        int gap = 24;

        BSPlayerStats stats = null;
        if (this.minecraft != null && this.minecraft.player != null) {
            stats = this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        }
        fieldLevel = createField(col1X, startY, stats != null ? String.valueOf(stats.level) : "1");
        fieldHp    = createField(col1X, startY + gap, stats != null ? String.valueOf((int)stats.bonusHp) : "0");
        fieldMp    = createField(col1X, startY + gap * 2, stats != null ? String.valueOf((int)stats.bonusMp) : "0");
        fieldAtk   = createField(col1X, startY + gap * 3, stats != null ? String.valueOf((int)stats.bonusAtk) : "0");
        fieldDef   = createField(col1X, startY + gap * 4, stats != null ? String.valueOf((int)stats.bonusDef) : "0");
        fieldMAtk  = createField(col1X, startY + gap * 5, stats != null ? String.valueOf((int)stats.bonusMatk) : "0");
        fieldMDef  = createField(col2X, startY, stats != null ? String.valueOf((int)stats.bonusMdef) : "0");
        fieldLuck  = createField(col2X, startY + gap, stats != null ? String.valueOf((int)stats.bonusLuc) : "0");
        fieldSpeed = createField(col2X, startY + gap * 2, stats != null ? String.valueOf((int)stats.bonusSpeed) : "0");
        fieldSouls = createField(col2X, startY + gap * 3, stats != null ? String.valueOf(stats.souls) : "0");
        fieldSen   = createField(col2X, startY + gap * 4, stats != null ? String.valueOf(stats.sen) : "0");

        this.addRenderableWidget(new BSRMButton(guiLeft + 70, guiTop + 185, 80, 20, Component.translatable("gui.blacksouls.dev_panel.apply"), btn -> sendPacket()));
        this.addRenderableWidget(new BSRMButton(guiLeft + 190, guiTop + 185, 80, 20, Component.translatable("gui.blacksouls.dev_panel.reset"), btn -> {
            fieldLevel.setValue("1");
            fieldHp.setValue("0"); fieldMp.setValue("0");
            fieldAtk.setValue("0"); fieldDef.setValue("0");
            fieldMAtk.setValue("0"); fieldMDef.setValue("0");
            fieldLuck.setValue("0"); fieldSpeed.setValue("0");
            fieldSouls.setValue("0"); fieldSen.setValue("0");
            sendPacket();
        }));
    }

    private EditBox createField(int x, int y, String value) {
        EditBox box = new EditBox(this.font, x, y, 90, 16, Component.empty());
        box.setValue(value);
        this.addRenderableWidget(box);
        return box;
    }

    private void sendPacket() {
        try {
            int lv = parseInt(fieldLevel.getValue(), 1);
            double hp = parseDouble(fieldHp.getValue(), 0);
            double mp = parseDouble(fieldMp.getValue(), 0);
            double atk = parseDouble(fieldAtk.getValue(), 0);
            double def = parseDouble(fieldDef.getValue(), 0);
            double matk = parseDouble(fieldMAtk.getValue(), 0);
            double mdef = parseDouble(fieldMDef.getValue(), 0);
            double luck = parseDouble(fieldLuck.getValue(), 0);
            double speed = parseDouble(fieldSpeed.getValue(), 0);
            long souls = parseLong(fieldSouls.getValue(), 0L);
            int sen = parseInt(fieldSen.getValue(), 0);

            NetworkHandler.INSTANCE.sendToServer(new PacketDevSetStats(lv, hp, mp, atk, def, matk, mdef, luck, speed, souls, sen));

            if (this.minecraft != null && BlackSouls.CURSOR1_EVENT != null) {
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
                this.minecraft.setScreen(null);
            }
        } catch (Exception ignored) {}
    }

    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception ignored) { return def; } }
    private long parseLong(String s, long def) { try { return Long.parseLong(s); } catch (Exception ignored) { return def; } }
    private double parseDouble(String s, double def) { try { return Double.parseDouble(s); } catch (Exception ignored) { return def; } }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.blacksouls.dev_panel.title"), this.width / 2, guiTop + 15, 0xFF5555);

        int col1TextX = guiLeft + 15;
        int col2TextX = guiLeft + 165;
        int startY = guiTop + 39;
        int gap = 24;

        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.dev_panel.level"), col1TextX, startY, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.dev_panel.bonus_hp"), col1TextX, startY + gap, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.dev_panel.bonus_mp"), col1TextX, startY + gap * 2, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.dev_panel.bonus_atk"), col1TextX, startY + gap * 3, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.dev_panel.bonus_def"), col1TextX, startY + gap * 4, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.dev_panel.bonus_matk"), col1TextX, startY + gap * 5, 0xFF55FF);
        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.dev_panel.bonus_mdef"), col2TextX, startY, 0x55FFFF);
        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.dev_panel.bonus_luc"), col2TextX, startY + gap, 0xFFFF55);
        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.dev_panel.bonus_speed"), col2TextX, startY + gap * 2, 0x55FF55);
        guiGraphics.drawString(font, "Souls", col2TextX, startY + gap * 3, 0xFFFFFF);
        guiGraphics.drawString(font, "Sen", col2TextX, startY + gap * 4, 0x5555FF);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return false;
    }
}