package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.client.ClientStoryName;
import com.BlackSouls.BlackSoulsMod.client.render.FadedBannerRenderer;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundConfirmStoryNamePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class GuiStoryNameConfirm extends Screen {
    private static final int OPTION_WIDTH = 80;
    private static final int OPTION_HEIGHT = 64;
    private final String storyName;
    private int selection;

    public GuiStoryNameConfirm(String storyName) {
        super(Component.translatable("gui.blacksouls.story_name.confirm", storyName));
        this.storyName = storyName;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF000000);
        int bannerTop = height / 2 - 25;
        FadedBannerRenderer.draw(graphics, 0, bannerTop, width, bannerTop + 50);
        graphics.drawString(font, title, 28, bannerTop + 20, 0xFFFFFFFF, false);

        int optionLeft = width - OPTION_WIDTH;
        int optionTop = Math.min(height - OPTION_HEIGHT, height / 2 + 32);
        BSGuiUtils.drawRMWindow(graphics, optionLeft, optionTop, OPTION_WIDTH, OPTION_HEIGHT);
        int hovered = optionAt(mouseX, mouseY, optionLeft, optionTop);
        if (hovered >= 0) {
            selection = hovered;
        }
        renderOption(graphics, optionLeft, optionTop + 14, 0, "gui.blacksouls.option.yes");
        renderOption(graphics, optionLeft, optionTop + 38, 1, "gui.blacksouls.option.no");
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderOption(GuiGraphics graphics, int optionLeft, int y, int index, String key) {
        if (selection == index) {
            graphics.fill(optionLeft + 8, y - 2, width - 8, y + 12, 0x66FFFFFF);
        }
        graphics.drawCenteredString(font, Component.translatable(key), optionLeft + OPTION_WIDTH / 2, y, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        int optionLeft = width - OPTION_WIDTH;
        int optionTop = Math.min(height - OPTION_HEIGHT, height / 2 + 32);
        int clicked = optionAt(mouseX, mouseY, optionLeft, optionTop);
        if (clicked >= 0) {
            selection = clicked;
            choose();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            selection = 1;
            choose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP
            || keyCode == GLFW.GLFW_KEY_DOWN
            || keyCode == GLFW.GLFW_KEY_LEFT
            || keyCode == GLFW.GLFW_KEY_RIGHT) {
            selection = 1 - selection;
            ClientStoryName.playCursor();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER
            || keyCode == GLFW.GLFW_KEY_KP_ENTER
            || keyCode == GLFW.GLFW_KEY_SPACE) {
            choose();
            return true;
        }
        return true;
    }

    private int optionAt(double mouseX, double mouseY, int optionLeft, int optionTop) {
        if (mouseX < optionLeft + 5 || mouseX > width - 5) {
            return -1;
        }
        int yesY = optionTop + 14;
        int noY = optionTop + 38;
        if (mouseY >= yesY - 4 && mouseY <= yesY + 12) {
            return 0;
        }
        if (mouseY >= noY - 4 && mouseY <= noY + 12) {
            return 1;
        }
        return -1;
    }

    private void choose() {
        ClientStoryName.playCursor();
        if (minecraft == null) {
            return;
        }
        if (selection == 1) {
            minecraft.setScreen(new GuiStoryNameInput(storyName));
            return;
        }
        NetworkHandler.sendToServer(new ServerboundConfirmStoryNamePacket(storyName));
        ClientStoryName.finishLocally(storyName);
        minecraft.setScreen(null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
