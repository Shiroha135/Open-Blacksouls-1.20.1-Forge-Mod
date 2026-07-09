package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSetDifficulty;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSetExtraMode;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuiDifficulty extends Screen {

    private static final int GUI_WIDTH = 228;
    private static final int GUI_HEIGHT = 284;

    private int guiLeft;
    private int guiTop;

    public GuiDifficulty() {
        super(Component.translatable("gui.blacksouls.difficulty.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        this.clearWidgets();

        int btnWidth = 46;
        int btnHeight = 20;
        int startX = guiLeft + 25;
        int startY = guiTop + 50;

        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = startX + col * 50;
            int y = startY + row * 30;
            int level = i + 1;

            BSGhostButton button = new BSGhostButton(x, y, btnWidth, btnHeight,
                    Component.translatable("gui.blacksouls.difficulty.lv", level), press -> {
                if (this.minecraft != null) {
                    if (this.minecraft.player != null) {
                        this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 2.0F);
                    }
                    try {
                        NetworkHandler.INSTANCE.sendToServer(new PacketSetDifficulty(level));
                    } catch (Exception ignored) {
                    }
                    DifficultyManager.currentDifficulty = level;
                    this.init();
                }
            });

            double mult = DifficultyManager.getMultiplier(level);
            button.setTooltip(Tooltip.create(Component.translatable("gui.blacksouls.difficulty.multiplier", String.format("%.1f", mult))));
            this.addRenderableWidget(button);
        }

        int modeStartX = guiLeft + 20;
        int modeStartY = guiTop + 152;
        int modeButtonWidth = 58;
        int modeButtonHeight = 20;
        int modeGapX = 64;
        int modeGapY = 26;

        addModeButton(modeStartX, modeStartY, modeButtonWidth, modeButtonHeight,
                Component.translatable("gui.blacksouls.difficulty.mode_revenge_short"),
                getModeDisplayName(PacketSetExtraMode.ItemMode.REVENGE),
                DifficultyManager.revengeUnlocked, DifficultyManager.revengeMode, PacketSetExtraMode.ItemMode.REVENGE);
        addModeButton(modeStartX + modeGapX, modeStartY, modeButtonWidth, modeButtonHeight,
                Component.translatable("gui.blacksouls.difficulty.mode_death_short"),
                getModeDisplayName(PacketSetExtraMode.ItemMode.DEATH),
                DifficultyManager.deathUnlocked, DifficultyManager.deathMode, PacketSetExtraMode.ItemMode.DEATH);
        addModeButton(modeStartX + modeGapX * 2, modeStartY, modeButtonWidth, modeButtonHeight,
                Component.translatable("gui.blacksouls.difficulty.mode_legendary_short"),
                getModeDisplayName(PacketSetExtraMode.ItemMode.LEGENDARY),
                DifficultyManager.legendaryUnlocked, DifficultyManager.legendaryMode, PacketSetExtraMode.ItemMode.LEGENDARY);
        addModeButton(modeStartX + 32, modeStartY + modeGapY, modeButtonWidth, modeButtonHeight,
                Component.translatable("gui.blacksouls.difficulty.mode_malice_short"),
                getModeDisplayName(PacketSetExtraMode.ItemMode.MALICE),
                DifficultyManager.maliceUnlocked, DifficultyManager.maliceMode, PacketSetExtraMode.ItemMode.MALICE);
        addModeButton(modeStartX + 32 + modeGapX, modeStartY + modeGapY, modeButtonWidth, modeButtonHeight,
                Component.translatable("gui.blacksouls.difficulty.mode_eternity_short"),
                getModeDisplayName(PacketSetExtraMode.ItemMode.ETERNITY),
                DifficultyManager.eternityUnlocked, DifficultyManager.eternityMode, PacketSetExtraMode.ItemMode.ETERNITY);

        this.addRenderableWidget(new BSGhostButton(guiLeft + 74, guiTop + 250, 80, 20,
                Component.translatable("gui.blacksouls.difficulty.close"), button -> {
            if (this.minecraft != null) {
                if (com.BlackSouls.BlackSoulsMod.BlackSouls.CURSOR1_EVENT != null) {
                    this.minecraft.getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                    com.BlackSouls.BlackSoulsMod.BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F)
                    );
                }
                this.minecraft.setScreen(null);
            }
        }));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);

        guiGraphics.drawCenteredString(font, Component.translatable("gui.blacksouls.difficulty.title"), this.width / 2, guiTop + 15, 0xFF0000);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.blacksouls.difficulty.current", DifficultyManager.currentDifficulty), this.width / 2, guiTop + 30, 0xFFFF00);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.blacksouls.difficulty.modes"), this.width / 2, guiTop + 138, 0xFFCC66);
        drawCenteredWrappedText(guiGraphics, Component.literal(getActiveModesText()), this.width / 2, guiTop + 208, GUI_WIDTH - 20, 0xAAAAFF);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addModeButton(int x, int y, int width, int height, Component shortLabel, Component fullLabel,
                               boolean unlocked, boolean enabled, PacketSetExtraMode.ItemMode mode) {
        BSGhostButton button = new BSGhostButton(x, y, width, height, Component.literal(shortLabel.getString()), press -> {
            if (this.minecraft != null) {
                NetworkHandler.INSTANCE.sendToServer(new PacketSetExtraMode(mode, !enabled));
                applyLocalMode(mode, !enabled);
                this.init();
            }
        });
        button.active = unlocked;
        if (!unlocked) {
            button.setTooltip(Tooltip.create(Component.translatable("gui.blacksouls.difficulty.mode_locked_full", fullLabel)));
        } else {
            button.setTooltip(Tooltip.create(Component.translatable(
                    enabled ? "gui.blacksouls.difficulty.mode_on_full" : "gui.blacksouls.difficulty.mode_off_full",
                    fullLabel)));
        }
        this.addRenderableWidget(button);
    }

    private Component getModeDisplayName(PacketSetExtraMode.ItemMode mode) {
        return switch (mode) {
            case REVENGE -> Component.translatable("gui.blacksouls.difficulty.mode_revenge");
            case DEATH -> Component.translatable("gui.blacksouls.difficulty.mode_death");
            case LEGENDARY -> Component.translatable("gui.blacksouls.difficulty.mode_legendary");
            case MALICE -> Component.translatable("gui.blacksouls.difficulty.mode_malice");
            case ETERNITY -> Component.translatable("gui.blacksouls.difficulty.mode_eternity");
        };
    }

    private void applyLocalMode(PacketSetExtraMode.ItemMode mode, boolean enabled) {
        switch (mode) {
            case REVENGE -> DifficultyManager.revengeMode = enabled;
            case DEATH -> DifficultyManager.deathMode = enabled;
            case LEGENDARY -> DifficultyManager.legendaryMode = enabled;
            case MALICE -> DifficultyManager.maliceMode = enabled;
            case ETERNITY -> DifficultyManager.eternityMode = enabled;
        }
    }

    private String getActiveModesText() {
        StringBuilder builder = new StringBuilder(Component.translatable("gui.blacksouls.difficulty.active_modes_prefix").getString());
        boolean hasAny = false;
        if (DifficultyManager.revengeMode) {
            builder.append(Component.translatable("gui.blacksouls.difficulty.mode_revenge").getString()).append(' ');
            hasAny = true;
        }
        if (DifficultyManager.deathMode) {
            builder.append(Component.translatable("gui.blacksouls.difficulty.mode_death").getString()).append(' ');
            hasAny = true;
        }
        if (DifficultyManager.legendaryMode) {
            builder.append(Component.translatable("gui.blacksouls.difficulty.mode_legendary").getString()).append(' ');
            hasAny = true;
        }
        if (DifficultyManager.maliceMode) {
            builder.append(Component.translatable("gui.blacksouls.difficulty.mode_malice").getString()).append(' ');
            hasAny = true;
        }
        if (DifficultyManager.eternityMode) {
            builder.append(Component.translatable("gui.blacksouls.difficulty.mode_eternity").getString()).append(' ');
            hasAny = true;
        }
        if (!hasAny) {
            builder.append(Component.translatable("gui.blacksouls.difficulty.none").getString());
        }
        return builder.toString().trim();
    }

    private void drawCenteredWrappedText(GuiGraphics guiGraphics, Component text, int centerX, int startY, int maxWidth, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(text, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.drawCenteredString(this.font, lines.get(i), centerX, startY + i * (this.font.lineHeight + 1), color);
        }
    }
}
