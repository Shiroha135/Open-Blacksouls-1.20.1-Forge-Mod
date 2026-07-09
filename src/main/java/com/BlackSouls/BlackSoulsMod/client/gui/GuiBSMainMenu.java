package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.ModListScreen;
import org.jetbrains.annotations.NotNull;

public class GuiBSMainMenu extends Screen {

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation(BlackSouls.MODID, "textures/gui/main_menu_bg.png");

    private static final int RM_BOX_WIDTH = 148;
    private static final int RM_BOX_HEIGHT = 126;

    private int rmBoxX;
    private int rmBoxY;

    public GuiBSMainMenu() {
        super(Component.literal("Black Souls Main Menu"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        this.rmBoxX = (this.width - RM_BOX_WIDTH) / 2;
        this.rmBoxY = Math.max(this.height / 2 + 5, this.height - RM_BOX_HEIGHT - 20);

        int btnWidth = 140;
        int btnHeight = 24;
        int startX = this.rmBoxX + (RM_BOX_WIDTH - btnWidth) / 2;
        int startY = this.rmBoxY + 3;

        this.addRenderableWidget(new BSMenuTextButton(startX, startY, btnWidth, btnHeight, Component.literal("SINGLEPLAYER"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(new SelectWorldScreen(this));
        }));
        this.addRenderableWidget(new BSMenuTextButton(startX, startY + 24, btnWidth, btnHeight, Component.literal("MULTIPLAYER"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(new JoinMultiplayerScreen(this));
        }));
        this.addRenderableWidget(new BSMenuTextButton(startX, startY + 48, btnWidth, btnHeight, Component.literal("OPTIONS"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
        }));
        this.addRenderableWidget(new BSMenuTextButton(startX, startY + 72, btnWidth, btnHeight, Component.literal("MODS"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(new ModListScreen(this));
        }));
        this.addRenderableWidget(new BSMenuTextButton(startX, startY + 96, btnWidth, btnHeight, Component.literal("QUIT GAME"), btn -> {
            if (this.minecraft != null) this.minecraft.stop();
        }));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        
        guiGraphics.blit(BG_TEXTURE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0F, 2.0F, 1.0F);

        BSGuiUtils.drawRMWindow(guiGraphics, this.rmBoxX / 2, this.rmBoxY / 2, RM_BOX_WIDTH / 2, RM_BOX_HEIGHT / 2);

        guiGraphics.pose().popPose();

        
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        
        String authorText = "By @BiliBili HatsuYukiAya";
        guiGraphics.drawString(this.font, authorText, this.width - this.font.width(authorText) - 2, this.height - 10, 0xFFFFFF, true);
    }
}