package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.client.WindowBranding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class GuiBSConfig extends Screen {
    private static final int MAX_PANEL_WIDTH = 520;
    private static final int MAX_PANEL_HEIGHT = 560;
    private static final int PANEL_MARGIN = 20;
    private static final int HEADER_HEIGHT = 54;
    private static final int FOOTER_HEIGHT = 48;
    private static final int CONTENT_PADDING = 16;
    private static final int ENTRY_HEIGHT = 50;
    private static final int ENTRY_STRIDE = 58;
    private static final int STATE_BUTTON_HEIGHT = 24;

    private final Screen parent;
    private final List<OptionRow> rows = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int contentBottom;
    private int contentHeight;
    private int scrollOffset;
    private int gameplaySectionY;
    private int clientSectionY;
    private int stateButtonWidth;
    private BSGhostButton saveButton;
    private BSGhostButton cancelButton;
    private boolean allowPlayerExtraModes;
    private boolean enableLowSenJumpscare;
    private boolean showCombatDamageChat;
    private boolean enableOriginalWindowBranding;
    private boolean enableMmdModels;
    private boolean enableCustomHealthBar;
    private BSConfig.CombatMode combatMode;

    public GuiBSConfig(Screen parent) {
        super(Component.translatable("gui.blacksouls.config.title"));
        this.parent = parent;
        this.allowPlayerExtraModes = BSConfig.ALLOW_PLAYER_EXTRA_MODES.get();
        this.enableLowSenJumpscare = BSConfig.ENABLE_LOW_SEN_JUMPSCARE.get();
        this.showCombatDamageChat = BSConfig.SHOW_COMBAT_DAMAGE_CHAT.get();
        this.enableOriginalWindowBranding = BSConfig.ENABLE_ORIGINAL_WINDOW_BRANDING.get();
        this.enableMmdModels = BSConfig.ENABLE_MMD_MODELS.get();
        this.enableCustomHealthBar = BSConfig.ENABLE_CUSTOM_HEALTH_BAR.get();
        this.combatMode = BSConfig.COMBAT_MODE.get();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.rows.clear();

        this.panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(280, this.width - PANEL_MARGIN * 2));
        this.panelHeight = Math.min(MAX_PANEL_HEIGHT, Math.max(220, this.height - PANEL_MARGIN));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.contentTop = this.panelY + HEADER_HEIGHT;
        this.contentBottom = this.panelY + this.panelHeight - FOOTER_HEIGHT;
        this.stateButtonWidth = Math.max(92, Math.min(132, this.panelWidth / 3));

        int y = 8;
        this.gameplaySectionY = y;
        y += 24;

        addOptionRow(y, "gui.blacksouls.config.option.extra_modes",
                "gui.blacksouls.config.allow_extra_modes.desc",
                stateText(this.allowPlayerExtraModes), () -> this.allowPlayerExtraModes,
                button -> {
                    this.allowPlayerExtraModes = !this.allowPlayerExtraModes;
                    button.setMessage(stateText(this.allowPlayerExtraModes));
                });
        y += ENTRY_STRIDE;

        addOptionRow(y, "gui.blacksouls.config.option.low_sen_jumpscare",
                "gui.blacksouls.config.low_sen_jumpscare.desc",
                stateText(this.enableLowSenJumpscare), () -> this.enableLowSenJumpscare,
                button -> {
                    this.enableLowSenJumpscare = !this.enableLowSenJumpscare;
                    button.setMessage(stateText(this.enableLowSenJumpscare));
                });
        y += ENTRY_STRIDE;

        addOptionRow(y, "gui.blacksouls.config.option.combat_damage_chat",
                "gui.blacksouls.config.combat_damage_chat.desc",
                stateText(this.showCombatDamageChat), () -> this.showCombatDamageChat,
                button -> {
                    this.showCombatDamageChat = !this.showCombatDamageChat;
                    button.setMessage(stateText(this.showCombatDamageChat));
                });
        y += ENTRY_STRIDE;

        addOptionRow(y, "gui.blacksouls.config.option.combat_mode",
                "gui.blacksouls.config.combat_mode.desc",
                combatModeText(),
                () -> this.combatMode == BSConfig.CombatMode.BLACK_SOULS_TURN_BASED,
                button -> {
                    this.combatMode = this.combatMode == BSConfig.CombatMode.BLACK_SOULS_TURN_BASED
                            ? BSConfig.CombatMode.MINECRAFT_REALTIME
                            : BSConfig.CombatMode.BLACK_SOULS_TURN_BASED;
                    button.setMessage(combatModeText());
                });
        y += ENTRY_STRIDE + 10;

        this.clientSectionY = y;
        y += 24;

        addOptionRow(y, "gui.blacksouls.config.option.window_branding",
                "gui.blacksouls.config.window_branding.desc",
                stateText(this.enableOriginalWindowBranding), () -> this.enableOriginalWindowBranding,
                button -> {
                    this.enableOriginalWindowBranding = !this.enableOriginalWindowBranding;
                    button.setMessage(stateText(this.enableOriginalWindowBranding));
                });
        y += ENTRY_STRIDE;

        addOptionRow(y, "gui.blacksouls.config.option.mmd_models",
                "gui.blacksouls.config.mmd_models.desc",
                stateText(this.enableMmdModels), () -> this.enableMmdModels,
                button -> {
                    this.enableMmdModels = !this.enableMmdModels;
                    button.setMessage(stateText(this.enableMmdModels));
                });
        y += ENTRY_STRIDE;

        addOptionRow(y, "gui.blacksouls.config.option.custom_health_bar",
                "gui.blacksouls.config.custom_health_bar.desc",
                stateText(this.enableCustomHealthBar), () -> this.enableCustomHealthBar,
                button -> {
                    this.enableCustomHealthBar = !this.enableCustomHealthBar;
                    button.setMessage(stateText(this.enableCustomHealthBar));
                });
        y += ENTRY_STRIDE;

        this.contentHeight = y + 6;
        this.scrollOffset = clampScroll(this.scrollOffset);

        int footerY = this.panelY + this.panelHeight - 34;
        int footerButtonWidth = Math.min(120, Math.max(84, (this.panelWidth - 54) / 2));
        int gap = 12;
        int totalWidth = footerButtonWidth * 2 + gap;
        int footerX = this.panelX + (this.panelWidth - totalWidth) / 2;

        this.saveButton = this.addRenderableWidget(new BSGhostButton(
                footerX, footerY, footerButtonWidth, 22,
                Component.translatable("gui.blacksouls.config.save"), button -> saveAndClose()));
        this.cancelButton = this.addRenderableWidget(new BSGhostButton(
                footerX + footerButtonWidth + gap, footerY, footerButtonWidth, 22,
                Component.translatable("gui.blacksouls.config.cancel"), button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parent);
                    }
                }));

        updateScrollableWidgets();
    }

    private void addOptionRow(int logicalY, String labelKey, String descriptionKey,
                              Component state, BooleanSupplier activeState,
                              Button.OnPress onPress) {
        ConfigStateButton button = new ConfigStateButton(
                0, 0, this.stateButtonWidth, STATE_BUTTON_HEIGHT,
                state, onPress, activeState);
        button.setTooltip(Tooltip.create(Component.translatable(descriptionKey)));
        this.addRenderableWidget(button);
        this.rows.add(new OptionRow(logicalY, labelKey, descriptionKey, button));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0x36000000);

        drawPanelShadow(graphics);
        BSGuiUtils.drawRMWindow(graphics, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        drawHeader(graphics);
        drawScrollableContent(graphics, mouseX, mouseY);
        drawFooter(graphics);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void drawPanelShadow(GuiGraphics graphics) {
        graphics.fill(this.panelX - 4, this.panelY + 5,
                this.panelX + this.panelWidth + 4, this.panelY + this.panelHeight + 5,
                0x66000000);
    }

    private void drawHeader(GuiGraphics graphics) {
        int accentX = this.panelX + 18;
        graphics.fill(accentX, this.panelY + 14, accentX + 3, this.panelY + 38, 0xFFD3A84F);
        graphics.drawString(this.font, this.title,
                accentX + 10, this.panelY + 14, 0xFFF2E7D5, false);
        graphics.drawString(this.font, Component.translatable("gui.blacksouls.config.subtitle"),
                accentX + 10, this.panelY + 30, 0xFF9E9297, false);
        graphics.fill(this.panelX + 14, this.contentTop - 3,
                this.panelX + this.panelWidth - 14, this.contentTop - 2, 0x555C3E36);

        if (maxScroll() > 0) {
            Component hint = Component.translatable("gui.blacksouls.config.scroll_hint");
            graphics.drawString(this.font, hint,
                    this.panelX + this.panelWidth - 18 - this.font.width(hint),
                    this.panelY + 30, 0xFF766D72, false);
        }
    }

    private void drawScrollableContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int clipLeft = this.panelX + 9;
        int clipRight = this.panelX + this.panelWidth - 9;
        graphics.enableScissor(clipLeft, this.contentTop, clipRight, this.contentBottom);

        drawSection(graphics, this.gameplaySectionY,
                Component.translatable("gui.blacksouls.config.section_gameplay"));
        drawSection(graphics, this.clientSectionY,
                Component.translatable("gui.blacksouls.config.section_client"));

        for (OptionRow row : this.rows) {
            drawOptionRow(graphics, row, mouseX, mouseY);
        }
        graphics.disableScissor();

        drawScrollbar(graphics);
    }

    private void drawSection(GuiGraphics graphics, int logicalY, Component title) {
        int y = contentY(logicalY);
        if (y < this.contentTop - 16 || y > this.contentBottom) {
            return;
        }
        int x = this.panelX + CONTENT_PADDING;
        graphics.drawString(this.font, title, x, y + 3, 0xFFE0B85E, false);
        int lineX = x + this.font.width(title) + 10;
        int lineRight = this.panelX + this.panelWidth - CONTENT_PADDING;
        if (lineRight > lineX) {
            graphics.fill(lineX, y + 8, lineRight, y + 9, 0x445A4546);
        }
    }

    private void drawOptionRow(GuiGraphics graphics, OptionRow row, int mouseX, int mouseY) {
        int y = contentY(row.logicalY());
        if (y + ENTRY_HEIGHT < this.contentTop || y > this.contentBottom) {
            return;
        }

        int x = this.panelX + CONTENT_PADDING;
        int width = this.panelWidth - CONTENT_PADDING * 2;
        boolean hovered = mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + ENTRY_HEIGHT
                && mouseY >= this.contentTop && mouseY < this.contentBottom;
        boolean enabled = row.button().stateEnabled();

        int fill = hovered ? 0x9A1B1518 : 0x77130F12;
        graphics.fill(x, y, x + width, y + ENTRY_HEIGHT, fill);
        graphics.fill(x, y, x + 2, y + ENTRY_HEIGHT,
                enabled ? 0xFFD0A34B : 0xFF5D5054);
        graphics.fill(x + 2, y, x + width, y + 1, hovered ? 0x665F4A49 : 0x3344383B);
        graphics.fill(x + 2, y + ENTRY_HEIGHT - 1, x + width, y + ENTRY_HEIGHT,
                hovered ? 0x665F4A49 : 0x3344383B);

        int textX = x + 11;
        graphics.drawString(this.font, Component.translatable(row.labelKey()),
                textX, y + 7, 0xFFF0E8DF, false);

        int textWidth = Math.max(60, width - this.stateButtonWidth - 34);
        List<FormattedCharSequence> lines = this.font.split(
                Component.translatable(row.descriptionKey()), textWidth);
        int descY = y + 22;
        int lineCount = Math.min(2, lines.size());
        for (int i = 0; i < lineCount; i++) {
            graphics.drawString(this.font, lines.get(i), textX,
                    descY + i * (this.font.lineHeight + 1), 0xFF9E9498, false);
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int trackX = this.panelX + this.panelWidth - 7;
        int trackTop = this.contentTop + 4;
        int trackBottom = this.contentBottom - 4;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        graphics.fill(trackX, trackTop, trackX + 2, trackBottom, 0x55352A2D);

        int viewport = Math.max(1, this.contentBottom - this.contentTop);
        int thumbHeight = Math.max(22,
                Math.round(trackHeight * (viewport / (float) this.contentHeight)));
        int travel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = trackTop + Math.round(travel * (this.scrollOffset / (float) maxScroll));
        graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xB8B28A49);
    }

    private void drawFooter(GuiGraphics graphics) {
        int footerTop = this.contentBottom;
        graphics.fill(this.panelX + 14, footerTop,
                this.panelX + this.panelWidth - 14, footerTop + 1, 0x555C3E36);
        drawFooterButtonBackground(graphics, this.saveButton, true);
        drawFooterButtonBackground(graphics, this.cancelButton, false);
    }

    private void drawFooterButtonBackground(GuiGraphics graphics, BSGhostButton button, boolean primary) {
        if (button == null) {
            return;
        }
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        boolean hovered = button.isHoveredOrFocused();
        int fill = primary
                ? (hovered ? 0x664D3718 : 0x4437271A)
                : (hovered ? 0x5540393C : 0x33262023);
        int border = primary ? 0xAACC9D4A : 0x8865585D;
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(x, y, x + 1, y + height, border);
        graphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0.0D && maxScroll() > 0
                && mouseX >= this.panelX && mouseX < this.panelX + this.panelWidth
                && mouseY >= this.contentTop && mouseY < this.contentBottom) {
            int next = clampScroll(this.scrollOffset - (int) Math.signum(delta) * 36);
            if (next != this.scrollOffset) {
                this.scrollOffset = next;
                updateScrollableWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void updateScrollableWidgets() {
        int buttonX = this.panelX + this.panelWidth
                - CONTENT_PADDING - this.stateButtonWidth - 8;
        for (OptionRow row : this.rows) {
            ConfigStateButton button = row.button();
            int y = contentY(row.logicalY())
                    + (ENTRY_HEIGHT - STATE_BUTTON_HEIGHT) / 2;
            button.setX(buttonX);
            button.setY(y);
            button.visible = y >= this.contentTop + 1
                    && y + STATE_BUTTON_HEIGHT <= this.contentBottom - 1;
        }
    }

    private int contentY(int logicalY) {
        return this.contentTop + logicalY - this.scrollOffset;
    }

    private int maxScroll() {
        return Math.max(0,
                this.contentHeight - Math.max(1, this.contentBottom - this.contentTop));
    }

    private int clampScroll(int value) {
        return Math.max(0, Math.min(maxScroll(), value));
    }

    private void saveAndClose() {
        BSConfig.ALLOW_PLAYER_EXTRA_MODES.set(this.allowPlayerExtraModes);
        BSConfig.ENABLE_LOW_SEN_JUMPSCARE.set(this.enableLowSenJumpscare);
        BSConfig.SHOW_COMBAT_DAMAGE_CHAT.set(this.showCombatDamageChat);
        BSConfig.COMBAT_MODE.set(this.combatMode);
        BSConfig.ENABLE_ORIGINAL_WINDOW_BRANDING.set(this.enableOriginalWindowBranding);
        BSConfig.ENABLE_MMD_MODELS.set(this.enableMmdModels);
        BSConfig.ENABLE_CUSTOM_HEALTH_BAR.set(this.enableCustomHealthBar);
        BSConfig.COMMON_SPEC.save();
        BSConfig.CLIENT_SPEC.save();
        WindowBranding.apply(this.enableOriginalWindowBranding);
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private Component stateText(boolean enabled) {
        return Component.translatable(enabled
                ? "gui.blacksouls.config.state.on"
                : "gui.blacksouls.config.state.off");
    }

    private Component combatModeText() {
        return Component.translatable(this.combatMode == BSConfig.CombatMode.BLACK_SOULS_TURN_BASED
                ? "gui.blacksouls.config.state.turn_based"
                : "gui.blacksouls.config.state.realtime");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record OptionRow(int logicalY, String labelKey, String descriptionKey,
                             ConfigStateButton button) {
    }

    private static final class ConfigStateButton extends BSGhostButton {
        private final BooleanSupplier stateSupplier;

        private ConfigStateButton(int x, int y, int width, int height,
                                  Component message, Button.OnPress onPress,
                                  BooleanSupplier stateSupplier) {
            super(x, y, width, height, message, onPress);
            this.stateSupplier = stateSupplier;
        }

        private boolean stateEnabled() {
            return this.stateSupplier.getAsBoolean();
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY,
                                    float partialTicks) {
            int x = this.getX();
            int y = this.getY();
            boolean enabled = stateEnabled();
            boolean hovered = this.isHoveredOrFocused();
            int fill = enabled
                    ? (hovered ? 0x7A49351C : 0x55402E1B)
                    : (hovered ? 0x6641393D : 0x44302A2D);
            int border = enabled ? 0xD0D2A34A : 0xA06B5E63;

            graphics.fill(x, y, x + this.width, y + this.height, fill);
            graphics.fill(x, y, x + this.width, y + 1, border);
            graphics.fill(x, y + this.height - 1, x + this.width, y + this.height, border);
            graphics.fill(x, y, x + 1, y + this.height, border);
            graphics.fill(x + this.width - 1, y, x + this.width, y + this.height, border);
            graphics.fill(x + 5, y + 5, x + 7, y + this.height - 5,
                    enabled ? 0xFFD7AB50 : 0xFF6A5B60);

            int textColor = enabled ? 0xFFF2D783 : 0xFFB7ADB1;
            if (hovered) {
                textColor = enabled ? 0xFFFFE6A0 : 0xFFD6CDD0;
            }
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                    x + this.width / 2 + 2,
                    y + (this.height - Minecraft.getInstance().font.lineHeight) / 2,
                    textColor);
        }
    }
}
