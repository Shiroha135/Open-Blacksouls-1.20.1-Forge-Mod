package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundWhiteBearActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class GuiWhiteBearDialogue extends Screen {
    private static final int MAIN_BOX_H = 90;
    private static final int NAME_BOX_H = 22;
    private static final int LINE_HEIGHT = 14;
    private static final int OPTION_BOX_WIDTH = 116;
    private static final int OPTION_LINE_HEIGHT = 16;
    private static final int OPTION_PADDING = 10;

    private final List<String> dialogueKeys = new ArrayList<>();
    private int lineIndex;
    private int charIndex;
    private int tickCounter;
    private boolean options;
    private boolean closeAfterDialogue;
    private boolean exitDialogue;

    public GuiWhiteBearDialogue(boolean firstVisit, boolean freeSouls, int progress) {
        super(Component.translatable("gui.blacksouls.white_bear.name"));
        if (firstVisit) {
            for (int i = 1; i <= 4; i++) {
                dialogueKeys.add("dialogue.blacksouls.white_bear.intro_" + i);
            }
            closeAfterDialogue = true;
        } else {
            dialogueKeys.add("dialogue.blacksouls.white_bear.greeting");
            if (freeSouls) {
                dialogueKeys.add("dialogue.blacksouls.white_bear.free_souls_1");
                dialogueKeys.add("dialogue.blacksouls.white_bear.free_souls_2");
            }
            updateOptionsForCurrentLine();
        }
    }

    @Override
    public void tick() {
        if (lineIndex >= dialogueKeys.size()) {
            return;
        }
        String text = currentText();
        int speed = Screen.hasControlDown() ? 8 : 1;
        if (charIndex < text.length()) {
            tickCounter += speed;
            if (tickCounter >= 2) {
                charIndex++;
                tickCounter = 0;
                if (!Screen.hasControlDown()) {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.2F)
                    );
                }
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int mainTop = height - MAIN_BOX_H;
        BSGuiUtils.drawRMWindow(guiGraphics, 0, mainTop, width, MAIN_BOX_H);

        int nameWidth = font.width(title) + 24;
        int nameTop = mainTop - 10;
        BSGuiUtils.drawRMWindow(guiGraphics, 15, nameTop, nameWidth, NAME_BOX_H);
        guiGraphics.drawString(font, title, 27, nameTop + 7, 0xFFFFFF, false);

        if (lineIndex < dialogueKeys.size()) {
            String full = currentText();
            int visibleLength = Math.min(charIndex, full.length());
            String visible = full.substring(0, visibleLength);
            List<FormattedCharSequence> lines = font.split(Component.literal(visible), width - 46);
            for (int i = 0; i < lines.size(); i++) {
                guiGraphics.drawString(font, lines.get(i), 23, mainTop + 14 + i * LINE_HEIGHT, 0xFFFFFF, false);
            }
            if (!options && charIndex >= full.length() && (net.minecraft.Util.getMillis() / 300L) % 2L == 0L) {
                guiGraphics.drawString(font, "▼", width - 27, mainTop + MAIN_BOX_H - 17, 0xAAAAAA, false);
            }
        }
        if (options) {
            renderOptions(guiGraphics, mouseX, mouseY, mainTop);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderOptions(GuiGraphics guiGraphics, int mouseX, int mouseY, int mainTop) {
        int boxHeight = OPTION_PADDING * 2 + OPTION_LINE_HEIGHT * 4;
        int left = width - OPTION_BOX_WIDTH;
        int top = mainTop - boxHeight;
        BSGuiUtils.drawRMWindow(guiGraphics, left, top, OPTION_BOX_WIDTH, boxHeight);
        String[] keys = {
                "gui.blacksouls.white_bear.option.buy",
                "gui.blacksouls.white_bear.option.reinforce",
                "gui.blacksouls.white_bear.option.storage",
                "gui.blacksouls.white_bear.option.exit"
        };
        for (int i = 0; i < keys.length; i++) {
            int y = top + OPTION_PADDING + i * OPTION_LINE_HEIGHT;
            boolean hovered = mouseX >= left + 5 && mouseX <= width - 5 && mouseY >= y && mouseY < y + OPTION_LINE_HEIGHT;
            if (hovered) {
                guiGraphics.fill(left + 5, y + 1, width - 5, y + OPTION_LINE_HEIGHT - 1, 0x66FFFFFF);
            }
            guiGraphics.drawString(font, Component.translatable(keys[i]), left + 10, y + 3, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (!options) {
            advance();
            return true;
        }

        int mainTop = height - MAIN_BOX_H;
        int boxHeight = OPTION_PADDING * 2 + OPTION_LINE_HEIGHT * 4;
        int left = width - OPTION_BOX_WIDTH;
        int top = mainTop - boxHeight;
        if (mouseX < left + 5 || mouseX > width - 5) {
            advance();
            return true;
        }
        int option = (int) ((mouseY - top - OPTION_PADDING) / OPTION_LINE_HEIGHT);
        if (option < 0 || option > 3) {
            advance();
            return true;
        }
        playCursor();
        if (option == 0) {
            minecraft.setScreen(new GuiUniversalShop(GuiUniversalShop.ShopType.WHITE_BEAR));
        } else if (option == 1) {
            closeAfterDialogue = false;
            dialogueKeys.clear();
            dialogueKeys.add("dialogue.blacksouls.white_bear.reinforce_hint");
            lineIndex = 0;
            charIndex = 0;
            tickCounter = 0;
            options = true;
        } else if (option == 2) {
            NetworkHandler.sendToServer(new ServerboundWhiteBearActionPacket(ServerboundWhiteBearActionPacket.Action.STORAGE));
            onClose();
        } else {
            beginExitDialogue();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (exitDialogue) {
                advance();
            } else {
                playCursor();
                beginExitDialogue();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void advance() {
        String text = currentText();
        if (charIndex < text.length()) {
            charIndex = text.length();
            return;
        }
        if (lineIndex < dialogueKeys.size() - 1) {
            lineIndex++;
            charIndex = 0;
            tickCounter = 0;
            updateOptionsForCurrentLine();
            return;
        }
        if (closeAfterDialogue) {
            onClose();
        } else {
            options = true;
        }
    }

    private void updateOptionsForCurrentLine() {
        options = !closeAfterDialogue && lineIndex == dialogueKeys.size() - 1;
    }

    private void beginExitDialogue() {
        options = false;
        closeAfterDialogue = true;
        exitDialogue = true;
        dialogueKeys.clear();
        dialogueKeys.add("dialogue.blacksouls.white_bear.exit");
        lineIndex = 0;
        charIndex = 0;
        tickCounter = 0;
    }

    private String currentText() {
        if (lineIndex >= dialogueKeys.size()) {
            return "";
        }
        return I18n.get(dialogueKeys.get(lineIndex));
    }

    private void playCursor() {
        if (BlackSouls.CURSOR1_EVENT != null) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F)
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
