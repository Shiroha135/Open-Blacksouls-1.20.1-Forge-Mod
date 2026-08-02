package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.client.WindowBranding;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuiBSConfig extends Screen {

    private static final int GUI_WIDTH = 250;
    private static final int GUI_HEIGHT = 342;

    private final Screen parent;
    private int guiLeft;
    private int guiTop;
    private boolean allowPlayerExtraModes;
    private boolean enableLowSenJumpscare;
    private boolean showCombatDamageChat;
    private boolean enableOriginalWindowBranding;
    private BSConfig.CombatMode combatMode;

    public GuiBSConfig(Screen parent) {
        super(Component.translatable("gui.blacksouls.config.title"));
        this.parent = parent;
        this.allowPlayerExtraModes = BSConfig.ALLOW_PLAYER_EXTRA_MODES.get();
        this.enableLowSenJumpscare = BSConfig.ENABLE_LOW_SEN_JUMPSCARE.get();
        this.showCombatDamageChat = BSConfig.SHOW_COMBAT_DAMAGE_CHAT.get();
        this.enableOriginalWindowBranding = BSConfig.ENABLE_ORIGINAL_WINDOW_BRANDING.get();
        this.combatMode = BSConfig.COMBAT_MODE.get();
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        this.clearWidgets();

        BSGhostButton extraModesButton = new BSGhostButton(guiLeft + 20, guiTop + 58, 210, 20, getExtraModesText(), button -> {
            this.allowPlayerExtraModes = !this.allowPlayerExtraModes;
            button.setMessage(getExtraModesText());
        });
        extraModesButton.setTooltip(Tooltip.create(Component.translatable("gui.blacksouls.config.allow_extra_modes.desc")));
        this.addRenderableWidget(extraModesButton);

        BSGhostButton jumpscareButton = new BSGhostButton(guiLeft + 20, guiTop + 104, 210, 20, getJumpscareText(), button -> {
            this.enableLowSenJumpscare = !this.enableLowSenJumpscare;
            button.setMessage(getJumpscareText());
        });
        jumpscareButton.setTooltip(Tooltip.create(Component.translatable("gui.blacksouls.config.low_sen_jumpscare.desc")));
        this.addRenderableWidget(jumpscareButton);

        BSGhostButton combatChatButton = new BSGhostButton(guiLeft + 20, guiTop + 150, 210, 20, getCombatDamageChatText(), button -> {
            this.showCombatDamageChat = !this.showCombatDamageChat;
            button.setMessage(getCombatDamageChatText());
        });
        combatChatButton.setTooltip(Tooltip.create(Component.translatable("gui.blacksouls.config.combat_damage_chat.desc")));
        this.addRenderableWidget(combatChatButton);

        BSGhostButton combatModeButton = new BSGhostButton(guiLeft + 20, guiTop + 196, 210, 20, getCombatModeText(), button -> {
            this.combatMode = this.combatMode == BSConfig.CombatMode.BLACK_SOULS_TURN_BASED
                    ? BSConfig.CombatMode.MINECRAFT_REALTIME
                    : BSConfig.CombatMode.BLACK_SOULS_TURN_BASED;
            button.setMessage(getCombatModeText());
        });
        combatModeButton.setTooltip(Tooltip.create(Component.translatable("gui.blacksouls.config.combat_mode.desc")));
        this.addRenderableWidget(combatModeButton);

        BSGhostButton windowBrandingButton = new BSGhostButton(guiLeft + 20, guiTop + 242, 210, 20, getWindowBrandingText(), button -> {
            this.enableOriginalWindowBranding = !this.enableOriginalWindowBranding;
            button.setMessage(getWindowBrandingText());
        });
        windowBrandingButton.setTooltip(Tooltip.create(Component.translatable("gui.blacksouls.config.window_branding.desc")));
        this.addRenderableWidget(windowBrandingButton);

        this.addRenderableWidget(new BSGhostButton(guiLeft + 42, guiTop + 304, 64, 20,
                Component.translatable("gui.blacksouls.config.save"), button -> {
            BSConfig.ALLOW_PLAYER_EXTRA_MODES.set(this.allowPlayerExtraModes);
            BSConfig.ENABLE_LOW_SEN_JUMPSCARE.set(this.enableLowSenJumpscare);
            BSConfig.SHOW_COMBAT_DAMAGE_CHAT.set(this.showCombatDamageChat);
            BSConfig.COMBAT_MODE.set(this.combatMode);
            BSConfig.ENABLE_ORIGINAL_WINDOW_BRANDING.set(this.enableOriginalWindowBranding);
            BSConfig.COMMON_SPEC.save();
            BSConfig.CLIENT_SPEC.save();
            WindowBranding.apply(this.enableOriginalWindowBranding);
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }));

        this.addRenderableWidget(new BSGhostButton(guiLeft + 144, guiTop + 304, 64, 20,
                Component.translatable("gui.blacksouls.config.cancel"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, guiTop + 16, 0xFFDDDDDD);
        guiGraphics.drawString(this.font, Component.translatable("gui.blacksouls.config.section_general"), guiLeft + 20, guiTop + 38, 0xFFFFD27A, false);
        drawWrappedText(guiGraphics, Component.translatable("gui.blacksouls.config.allow_extra_modes.desc"), guiLeft + 20, guiTop + 82, GUI_WIDTH - 40, 0xFFB8B8FF);
        drawWrappedText(guiGraphics, Component.translatable("gui.blacksouls.config.low_sen_jumpscare.desc"), guiLeft + 20, guiTop + 128, GUI_WIDTH - 40, 0xFFB8B8FF);
        drawWrappedText(guiGraphics, Component.translatable("gui.blacksouls.config.combat_damage_chat.desc"), guiLeft + 20, guiTop + 174, GUI_WIDTH - 40, 0xFFB8B8FF);
        drawWrappedText(guiGraphics, Component.translatable("gui.blacksouls.config.combat_mode.desc"), guiLeft + 20, guiTop + 220, GUI_WIDTH - 40, 0xFFB8B8FF);
        drawWrappedText(guiGraphics, Component.translatable("gui.blacksouls.config.window_branding.desc"), guiLeft + 20, guiTop + 266, GUI_WIDTH - 40, 0xFFB8B8FF);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component getExtraModesText() {
        return Component.translatable(this.allowPlayerExtraModes
                ? "gui.blacksouls.config.allow_extra_modes.on"
                : "gui.blacksouls.config.allow_extra_modes.off");
    }

    private Component getJumpscareText() {
        return Component.translatable(this.enableLowSenJumpscare
                ? "gui.blacksouls.config.low_sen_jumpscare.on"
                : "gui.blacksouls.config.low_sen_jumpscare.off");
    }

    private Component getCombatDamageChatText() {
        return Component.translatable(this.showCombatDamageChat
                ? "gui.blacksouls.config.combat_damage_chat.on"
                : "gui.blacksouls.config.combat_damage_chat.off");
    }

    private Component getCombatModeText() {
        return Component.translatable(this.combatMode == BSConfig.CombatMode.BLACK_SOULS_TURN_BASED
                ? "gui.blacksouls.config.combat_mode.turn_based"
                : "gui.blacksouls.config.combat_mode.realtime");
    }

    private Component getWindowBrandingText() {
        return Component.translatable(this.enableOriginalWindowBranding
                ? "gui.blacksouls.config.window_branding.on"
                : "gui.blacksouls.config.window_branding.off");
    }

    private void drawWrappedText(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(text, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.drawString(this.font, lines.get(i), x, y + i * (this.font.lineHeight + 1), color, false);
        }
    }
}
