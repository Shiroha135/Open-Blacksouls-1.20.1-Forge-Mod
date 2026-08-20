package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BonfireEntry;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketTeleportToBonfire;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public class GuiBonfireMenu extends Screen {

    private static final int GUI_WIDTH = 380;
    private static final int GUI_HEIGHT = 260;
    private static final int LIST_WIDTH = 120;
    private static final float MAP_HEIGHT_RATIO = 0.60F;

    private static final int ITEM_START_Y_OFFSET = 15;
    private static final int ITEM_LINE_HEIGHT = 20;
    private static final int SCROLL_PADDING = 30;
    private static final int TEXT_OFFSET_X = 12;
    private static final int SELECTION_BG_PADDING_X = 8;
    private static final int SELECTION_BG_PADDING_Y = 2;
    private static final int INFO_PADDING = 16;

    private static final int COLOR_TEXT_HIGHLIGHT = 0xFFFFFF;
    private static final int COLOR_TEXT_NORMAL = 0xFFFFFF;
    private static final int COLOR_SCROLL_ARROW = 0x888888;
    private static final int COLOR_SELECTION_BG = 0x33FFFFFF;

    private static final String SYMBOL_UP = "^";
    private static final String SYMBOL_DOWN = "v";
    private static final String UNNAMED_KEY = "gui.blacksouls.bonfire.unnamed";

    private static final ResourceLocation MAP_TEXTURE = new ResourceLocation(BlackSouls.MODID, "textures/gui/map.png");

    private final List<BonfireEntry> bonfires;
    private BonfireEntry selectedBonfire;
    private int guiLeft, guiTop;
    private int mapW, mapH, infoH;
    private int scrollOffset = 0;

    private EditBox nameField;
    private MultiLineEditBox descField;

    public GuiBonfireMenu(List<BonfireEntry> bonfires) {
        super(Component.translatable("gui.blacksouls.bonfire.title"));
        this.bonfires = new ArrayList<>(bonfires);
        if (!this.bonfires.isEmpty()) {
            this.selectedBonfire = this.bonfires.get(0);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        this.mapW = GUI_WIDTH - LIST_WIDTH;
        this.mapH = (int) (GUI_HEIGHT * MAP_HEIGHT_RATIO);
        this.infoH = GUI_HEIGHT - this.mapH;

        int infoX = guiLeft + LIST_WIDTH + INFO_PADDING;
        int infoY = guiTop + mapH + INFO_PADDING;
        int boxW = mapW - INFO_PADDING * 2;

        nameField = new EditBox(this.font, infoX, infoY, boxW, 16, Component.empty());
        nameField.setBordered(false);
        nameField.setTextColor(COLOR_TEXT_HIGHLIGHT);
        nameField.setTextColorUneditable(COLOR_TEXT_HIGHLIGHT);
        nameField.setMaxLength(50);
        nameField.setEditable(false);
        this.addRenderableWidget(nameField);

        descField = new MultiLineEditBox(this.font, infoX, infoY + 16, boxW, 56, Component.empty(), Component.translatable("gui.blacksouls.bonfire.desc_hint")) {
            @Override
            protected void renderBackground(@NotNull GuiGraphics guiGraphics) {
            }
        };
        descField.active = false;
        this.addRenderableWidget(descField);

        updateFields();
    }

    private String getDisplayName(String rawName) {
        if (rawName == null) return "";
        if (UNNAMED_KEY.equals(rawName) || rawName.startsWith("gui.blacksouls.")) {
            return I18n.get(rawName);
        }
        return rawName;
    }

    private String getDisplayDesc(String rawDesc) {
        if (rawDesc == null) return "";
        if (rawDesc.startsWith("gui.blacksouls.")) {
            return I18n.get(rawDesc);
        }
        return rawDesc;
    }

    private void updateFields() {
        if (selectedBonfire != null) {
            nameField.setValue("[" + getDisplayName(selectedBonfire.name) + "]");
            nameField.visible = true;

            String desc = getDisplayDesc(selectedBonfire.description);
            if (desc == null || desc.isBlank()) {
                descField.setValue("");
            } else {
                descField.setValue(desc);
            }
            descField.visible = true;
        } else {
            nameField.visible = false;
            descField.visible = false;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, LIST_WIDTH, GUI_HEIGHT);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft + LIST_WIDTH, guiTop, mapW, mapH);
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft + LIST_WIDTH, guiTop + mapH, mapW, infoH);

        int itemY = guiTop + ITEM_START_Y_OFFSET;
        int visibleCount = getVisibleItemCount();
        int itemX = guiLeft + TEXT_OFFSET_X;

        for (int i = 0; i < Math.min(bonfires.size() - scrollOffset, visibleCount); i++) {
            int dataIndex = i + scrollOffset;
            BonfireEntry bonfire = bonfires.get(dataIndex);

            int currentY = itemY + i * ITEM_LINE_HEIGHT;
            boolean isHovered = isMouseOverItem(mouseX, mouseY, currentY);
            boolean isSelected = selectedBonfire != null && bonfire.pos.equals(selectedBonfire.pos);

            int color = isSelected || isHovered ? COLOR_TEXT_HIGHLIGHT : COLOR_TEXT_NORMAL;

            guiGraphics.drawString(font, Component.literal("[" + getDisplayName(bonfire.name) + "]"), itemX, currentY, color, false);

            if (isSelected) {
                guiGraphics.fill(
                        guiLeft + SELECTION_BG_PADDING_X,
                        currentY - SELECTION_BG_PADDING_Y,
                        guiLeft + LIST_WIDTH - SELECTION_BG_PADDING_X,
                        currentY + ITEM_LINE_HEIGHT - SELECTION_BG_PADDING_Y,
                        COLOR_SELECTION_BG
                );
            }
        }

        int arrowX = guiLeft + LIST_WIDTH / 2;
        if (scrollOffset > 0) {
            guiGraphics.drawCenteredString(font, SYMBOL_UP, arrowX, guiTop + 6, COLOR_SCROLL_ARROW);
        }
        if (scrollOffset + visibleCount < bonfires.size()) {
            guiGraphics.drawCenteredString(font, SYMBOL_DOWN, arrowX, guiTop + GUI_HEIGHT - 14, COLOR_SCROLL_ARROW);
        }

        int borderGap = 8;
        int mapStartX = guiLeft + LIST_WIDTH + borderGap;
        int mapStartY = guiTop + borderGap;
        int drawMapW = mapW - borderGap * 2;
        int drawMapH = mapH - borderGap * 2;

        RenderSystem.enableBlend();
        guiGraphics.blit(MAP_TEXTURE, mapStartX, mapStartY, 0, 0, drawMapW, drawMapH, drawMapW, drawMapH);
        RenderSystem.disableBlend();

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (button == 0) {
            int itemY = guiTop + ITEM_START_Y_OFFSET;
            int visibleCount = getVisibleItemCount();

            for (int i = 0; i < Math.min(bonfires.size() - scrollOffset, visibleCount); i++) {
                int dataIndex = i + scrollOffset;
                int currentY = itemY + i * ITEM_LINE_HEIGHT;

                if (isMouseOverItem((int) mouseX, (int) mouseY, currentY)) {
                    if (this.minecraft != null && BlackSouls.CURSOR1_EVENT != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
                    }

                    BonfireEntry clickedEntry = bonfires.get(dataIndex);

                    if (selectedBonfire != null && clickedEntry.pos.equals(selectedBonfire.pos)) {
                        NetworkHandler.INSTANCE.sendToServer(new PacketTeleportToBonfire(clickedEntry.pos));
                        this.onClose();
                    } else {
                        this.selectedBonfire = clickedEntry;
                        updateFields();
                    }
                    return true;
                }
            }
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visibleCount = getVisibleItemCount();
        int maxOffset = Math.max(0, bonfires.size() - visibleCount);

        if (maxOffset > 0) {
            if (delta > 0) scrollOffset--;
            else if (delta < 0) scrollOffset++;
            scrollOffset = Mth.clamp(scrollOffset, 0, maxOffset);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameField.isFocused() || descField.isFocused()) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.setFocused(null);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int getVisibleItemCount() {
        return (GUI_HEIGHT - SCROLL_PADDING) / ITEM_LINE_HEIGHT;
    }

    private boolean isMouseOverItem(int mouseX, int mouseY, int itemY) {
        return mouseX >= guiLeft + TEXT_OFFSET_X
                && mouseX <= guiLeft + LIST_WIDTH - TEXT_OFFSET_X
                && mouseY >= itemY
                && mouseY <= itemY + ITEM_LINE_HEIGHT;
    }
}
