package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundSimpleActionPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiYouDied extends Screen {

    private int delayTicks = 0;
    private boolean respawnRequested = false;

    private static final float TEXT_SCALE = 3.5F;

    public GuiYouDied() {
        super(Component.translatable("gui.blacksouls.death.you_died"));
    }

    @Override
    public void tick() {
        super.tick();
        this.delayTicks++;

        if (this.minecraft != null && this.minecraft.player != null) {
            if (this.minecraft.player.isAlive()) {
                this.minecraft.setScreen(null);
                return;
            }

            if (this.delayTicks >= 20 && !this.respawnRequested) {
                requestRespawn();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int screenWidth = this.width;
        int screenHeight = this.height;

        int bannerHeight = (int)(screenHeight * 0.15F);
        int y1 = (int)(screenHeight * 0.33F);
        int y2 = y1 + bannerHeight;

        int color = 170 << 24;

        RenderSystem.enableBlend();
        graphics.fill(0, y1, screenWidth, y2, color);
        RenderSystem.disableBlend();

        graphics.pose().pushPose();
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);

        int textX = (int) (screenWidth / 2.0F / TEXT_SCALE);
        int textY = (int) ((y1 + (bannerHeight - this.font.lineHeight * TEXT_SCALE) / 2.0F) / TEXT_SCALE);

        int titleWidth = this.font.width(this.title);
        graphics.drawString(this.font, this.title, textX - titleWidth / 2, textY, 0xFF0000, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.delayTicks >= 20 && !this.respawnRequested && this.minecraft != null && this.minecraft.player != null) {
            requestRespawn();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.delayTicks >= 20 && !this.respawnRequested && this.minecraft != null && this.minecraft.player != null) {
            requestRespawn();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void requestRespawn() {
        this.respawnRequested = true;
        NetworkHandler.sendToServer(new ServerboundSimpleActionPacket(ServerboundSimpleActionPacket.Action.REQUEST_RESPAWN));
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
