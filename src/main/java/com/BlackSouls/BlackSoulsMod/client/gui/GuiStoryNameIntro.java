package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.client.ClientStoryName;
import com.BlackSouls.BlackSoulsMod.client.render.FadedBannerRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class GuiStoryNameIntro extends Screen {
    private final String candidate;

    public GuiStoryNameIntro(String candidate) {
        super(Component.translatable("gui.blacksouls.story_name.intro"));
        this.candidate = candidate;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF000000);
        int bannerTop = height / 2 - 25;
        FadedBannerRenderer.draw(graphics, 0, bannerTop, width, bannerTop + 50);
        graphics.drawString(font, title, 28, bannerTop + 20, 0xFFFFFFFF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            openInput();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            openInput();
            return true;
        }
        return true;
    }

    private void openInput() {
        ClientStoryName.playCursor();
        if (minecraft != null) {
            minecraft.setScreen(new GuiStoryNameInput(candidate));
        }
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
