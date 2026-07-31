package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.client.ClientStoryName;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundFogGateProceedPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class GuiFogGatePrompt extends Screen {
    private static final int BANNER_HEIGHT = 90;
    private static final int OPTION_WIDTH = 132;
    private static final int OPTION_HEIGHT = 52;
    private static final int OPTION_LINE_HEIGHT = 18;
    private final BlockPos gatePos;
    private int selection;

    public GuiFogGatePrompt(BlockPos gatePos) {
        super(Component.translatable("gui.blacksouls.fog_gate.prompt"));
        this.gatePos = gatePos.immutable();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bannerTop = height - BANNER_HEIGHT;
        graphics.fill(0, bannerTop, width, height, 0xE8000000);
        graphics.fill(0, bannerTop, width, bannerTop + 1, 0x669A8B6A);
        graphics.drawString(font, title, 16, bannerTop + 16, 0xFFFFFFFF, true);

        int optionLeft = width - OPTION_WIDTH;
        int optionTop = bannerTop - OPTION_HEIGHT;
        BSGuiUtils.drawRMWindow(graphics, optionLeft, optionTop, OPTION_WIDTH, OPTION_HEIGHT);
        int hovered = optionAt(mouseX, mouseY, optionLeft, optionTop);
        if (hovered >= 0) {
            selection = hovered;
        }
        renderOption(graphics, optionLeft, optionTop + 8, 0, "gui.blacksouls.fog_gate.proceed");
        renderOption(graphics, optionLeft, optionTop + 26, 1, "gui.blacksouls.fog_gate.leave");
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderOption(GuiGraphics graphics, int optionLeft, int y, int index, String key) {
        if (selection == index) {
            graphics.fill(optionLeft + 5, y + 1, width - 5, y + OPTION_LINE_HEIGHT - 1, 0x66FFFFFF);
        }
        graphics.drawString(font, Component.translatable(key), optionLeft + 10, y + 4, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        int optionLeft = width - OPTION_WIDTH;
        int optionTop = height - BANNER_HEIGHT - OPTION_HEIGHT;
        int clicked = optionAt(mouseX, mouseY, optionLeft, optionTop);
        if (clicked < 0) {
            return false;
        }
        selection = clicked;
        choose();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int optionAt(double mouseX, double mouseY, int optionLeft, int optionTop) {
        int listTop = optionTop + 8;
        if (mouseX < optionLeft + 5
                || mouseX > width - 5
                || mouseY < listTop
                || mouseY >= listTop + OPTION_LINE_HEIGHT * 2) {
            return -1;
        }
        return (int) ((mouseY - listTop) / OPTION_LINE_HEIGHT);
    }

    private void choose() {
        ClientStoryName.playCursor();
        if (minecraft == null) {
            return;
        }
        if (selection == 0) {
            NetworkHandler.sendToServer(new ServerboundFogGateProceedPacket(gatePos));
        }
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
