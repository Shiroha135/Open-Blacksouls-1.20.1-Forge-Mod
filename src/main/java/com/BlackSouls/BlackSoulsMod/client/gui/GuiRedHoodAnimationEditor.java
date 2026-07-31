package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundSetRedHoodAnimationPacket;
import com.shiroha.mmdskin.asset.catalog.AnimationInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class GuiRedHoodAnimationEditor extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 280;
    private static final int PADDING = 16;
    private static final int ROW_HEIGHT = 20;
    private final int entityId;
    private final String currentAnimation;
    private final List<ActionEntry> animations;
    private List<ActionEntry> filteredAnimations;
    private EditBox searchField;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listBottom;
    private int scrollOffset;
    private int maxScroll;

    public GuiRedHoodAnimationEditor(int entityId, String currentAnimation) {
        super(Component.translatable("gui.blacksouls.red_hood.animation.title"));
        this.entityId = entityId;
        this.currentAnimation = currentAnimation == null ? "" : currentAnimation;
        LinkedHashMap<String, ActionEntry> unique = new LinkedHashMap<>();
        for (AnimationInfo animation : AnimationInfo.scanAnimationsForModel("小红帽")) {
            String name = animation.getAnimName();
            if (name != null && !name.isBlank()) {
                unique.putIfAbsent(name, new ActionEntry(name, animation.getDisplayName()));
            }
        }
        this.animations = List.copyOf(unique.values());
        this.filteredAnimations = this.animations;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(PANEL_WIDTH, this.width - 16);
        this.panelHeight = Math.min(PANEL_HEIGHT, this.height - 16);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.listTop = this.panelY + 72;
        this.listBottom = this.panelY + this.panelHeight - 40;

        this.searchField = new EditBox(
                this.font,
                this.panelX + PADDING,
                this.panelY + 48,
                this.panelWidth - PADDING * 2,
                18,
                Component.translatable("gui.blacksouls.red_hood.animation.search")
        );
        this.searchField.setHint(Component.translatable("gui.blacksouls.red_hood.animation.search"));
        this.searchField.setMaxLength(128);
        this.searchField.setResponder(this::filterAnimations);
        this.addRenderableWidget(this.searchField);

        int buttonY = this.panelY + this.panelHeight - 30;
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.blacksouls.red_hood.animation.restore"),
                button -> applyAnimation("")
        ).bounds(this.panelX + PADDING, buttonY, 120, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose()
        ).bounds(this.panelX + this.panelWidth - PADDING - 80, buttonY, 80, 20).build());
        updateMaxScroll();
    }

    private void filterAnimations(String query) {
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            this.filteredAnimations = this.animations;
        } else {
            List<ActionEntry> matches = new ArrayList<>();
            for (ActionEntry animation : this.animations) {
                if (animation.name.toLowerCase(Locale.ROOT).contains(normalized)
                        || animation.displayName.toLowerCase(Locale.ROOT).contains(normalized)) {
                    matches.add(animation);
                }
            }
            this.filteredAnimations = List.copyOf(matches);
        }
        this.scrollOffset = 0;
        updateMaxScroll();
    }

    private void updateMaxScroll() {
        this.maxScroll = Math.max(0, this.filteredAnimations.size() * ROW_HEIGHT
                - Math.max(0, this.listBottom - this.listTop));
        this.scrollOffset = Math.max(0, Math.min(this.maxScroll, this.scrollOffset));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        BSGuiUtils.drawRMWindow(graphics, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.panelY + 14, 0xFFFFFF);
        Component selected = this.currentAnimation.isBlank()
                ? Component.translatable("gui.blacksouls.red_hood.animation.idle")
                : Component.literal(this.currentAnimation);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.blacksouls.red_hood.animation.current", selected),
                this.width / 2,
                this.panelY + 30,
                0xC8C8C8
        );
        graphics.fill(
                this.panelX + PADDING,
                this.listTop,
                this.panelX + this.panelWidth - PADDING,
                this.listBottom,
                0x80000000
        );
        graphics.enableScissor(
                this.panelX + PADDING,
                this.listTop,
                this.panelX + this.panelWidth - PADDING,
                this.listBottom
        );
        renderAnimationRows(graphics, mouseX, mouseY);
        graphics.disableScissor();
        renderScrollbar(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderAnimationRows(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.panelX + PADDING + 2;
        int width = this.panelWidth - PADDING * 2 - 6;
        int y = this.listTop - this.scrollOffset;
        for (ActionEntry animation : this.filteredAnimations) {
            if (y + ROW_HEIGHT > this.listTop && y < this.listBottom) {
                boolean hovered = mouseX >= x && mouseX < x + width
                        && mouseY >= y && mouseY < y + ROW_HEIGHT;
                boolean selected = animation.name.equals(this.currentAnimation);
                int background = selected ? 0x705F1018 : hovered ? 0x504B4B4B : 0x28000000;
                graphics.fill(x, y, x + width, y + ROW_HEIGHT - 1, background);
                if (selected) {
                    graphics.fill(x, y, x + 2, y + ROW_HEIGHT - 1, 0xFFD88484);
                }
                String name = this.font.plainSubstrByWidth(animation.displayName, width / 2 - 10);
                String id = this.font.plainSubstrByWidth(animation.name, width / 2 - 10);
                graphics.drawString(this.font, name, x + 7, y + 6, 0xFFFFFF, false);
                graphics.drawString(
                        this.font,
                        id,
                        x + width - this.font.width(id) - 7,
                        y + 6,
                        0xFFAFAFAF,
                        false
                );
            }
            y += ROW_HEIGHT;
        }
        if (this.filteredAnimations.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.blacksouls.red_hood.animation.empty"),
                    this.width / 2,
                    this.listTop + 12,
                    0xFFAFAFAF
            );
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (this.maxScroll <= 0) {
            return;
        }
        int x = this.panelX + this.panelWidth - PADDING - 3;
        int height = this.listBottom - this.listTop;
        int thumbHeight = Math.max(12, height * height / (height + this.maxScroll));
        int thumbY = this.listTop + (height - thumbHeight) * this.scrollOffset / this.maxScroll;
        graphics.fill(x, this.listTop, x + 2, this.listBottom, 0x40404040);
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xFFD88484);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && mouseX >= this.panelX + PADDING
                && mouseX < this.panelX + this.panelWidth - PADDING
                && mouseY >= this.listTop
                && mouseY < this.listBottom) {
            int index = ((int) mouseY - this.listTop + this.scrollOffset) / ROW_HEIGHT;
            if (index >= 0 && index < this.filteredAnimations.size()) {
                applyAnimation(this.filteredAnimations.get(index).name);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= this.panelX + PADDING
                && mouseX < this.panelX + this.panelWidth - PADDING
                && mouseY >= this.listTop
                && mouseY < this.listBottom) {
            this.scrollOffset = Math.max(0, Math.min(this.maxScroll,
                    this.scrollOffset - (int) Math.signum(delta) * ROW_HEIGHT * 2));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void applyAnimation(String animation) {
        NetworkHandler.sendToServer(new ServerboundSetRedHoodAnimationPacket(this.entityId, animation));
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ActionEntry(String name, String displayName) {
    }
}
