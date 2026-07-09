package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BonfireEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

public class GuiBonfireRest extends Screen {

    private static final int GUI_WIDTH = 90;
    private static final int GUI_HEIGHT = 80;
    private static final int MARGIN_BOTTOM_RIGHT = 40;
    private static final int ITEM_HEIGHT = 24;
    private static final int START_Y_OFFSET = 18;
    private static final int PADDING_X = 10;
    private static final int TEXT_OFFSET_Y = 6;
    private static final int HIGHLIGHT_PADDING_Y = 2;
    private static final int COLOR_TEXT_HOVER = 0xFFFFFF;
    private static final int COLOR_TEXT_NORMAL = 0xAAAAAA;
    private static final int COLOR_HIGHLIGHT_BG = 0x22FFFFFF;
    private static final Component TITLE = Component.translatable("gui.blacksouls.bonfire_rest.title");

    private enum RestOption {
        TELEPORT("gui.blacksouls.bonfire_rest.teleport"),
        LEAVE("gui.blacksouls.bonfire_rest.leave");
        final Component text;
        RestOption(String translationKey) {
            this.text = Component.translatable(translationKey);
        }
    }

    private final List<BonfireEntry> bonfires;
    private int guiLeft;
    private int guiTop;

    public GuiBonfireRest(List<BonfireEntry> bonfires) {
        super(TITLE);
        this.bonfires = bonfires;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = this.width - GUI_WIDTH - MARGIN_BOTTOM_RIGHT;
        this.guiTop = this.height - GUI_HEIGHT - MARGIN_BOTTOM_RIGHT;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        BSGuiUtils.drawRMWindow(guiGraphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);

        RestOption[] options = RestOption.values();
        for (int i = 0; i < options.length; i++) {
            int currentY = guiTop + START_Y_OFFSET + i * ITEM_HEIGHT;
            boolean isHovered = isMouseOverItem(mouseX, mouseY, currentY);
            int color = isHovered ? COLOR_TEXT_HOVER : COLOR_TEXT_NORMAL;
            guiGraphics.drawCenteredString(font, options[i].text, guiLeft + GUI_WIDTH / 2, currentY + TEXT_OFFSET_Y, color);
            if (isHovered) {
                guiGraphics.fill(
                        guiLeft + PADDING_X,
                        currentY + HIGHLIGHT_PADDING_Y,
                        guiLeft + GUI_WIDTH - PADDING_X,
                        currentY + ITEM_HEIGHT - HIGHLIGHT_PADDING_Y,
                        COLOR_HIGHLIGHT_BG
                );
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null) {
            RestOption[] options = RestOption.values();

            for (int i = 0; i < options.length; i++) {
                int currentY = guiTop + START_Y_OFFSET + i * ITEM_HEIGHT;
                if (isMouseOverItem((int) mouseX, (int) mouseY, currentY)) {
                    if (BlackSouls.CURSOR1_EVENT != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
                    }

                    switch (options[i]) {
                        case TELEPORT -> this.minecraft.setScreen(new GuiBonfireMenu(bonfires));
                        case LEAVE -> this.onClose();
                    }
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
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isMouseOverItem(int mouseX, int mouseY, int currentY) {
        return mouseX >= guiLeft + PADDING_X
                && mouseX <= guiLeft + GUI_WIDTH - PADDING_X
                && mouseY >= currentY
                && mouseY < currentY + ITEM_HEIGHT;
    }
}
