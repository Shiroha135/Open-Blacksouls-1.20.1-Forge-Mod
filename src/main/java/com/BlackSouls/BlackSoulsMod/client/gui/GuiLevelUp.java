package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketConvertSouls;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

public class GuiLevelUp extends Screen {

    private int dialogueState = 0;
    private String playerName = "master";
    private long currentSouls = 0;
    private final int[] digits = new int[6];
    private int currentDigitIndex = 0;

    private final String npcAvatarId;

    private ResourceLocation avatarTex;
    private static final String KEY_NPC_NAME = "gui.blacksouls.npc.noden";
    private static final String KEY_DIALOG_WELCOME = "gui.blacksouls.levelup.dialog.welcome";
    private static final String KEY_DIALOG_CANCEL = "gui.blacksouls.levelup.dialog.cancel";
    private static final String KEY_DIALOG_CONFIRM = "gui.blacksouls.levelup.dialog.confirm";
    private static final int COLOR_TEXT_HIGHLIGHT = 0xFFFFFF;
    private static final int COLOR_TEXT_NORMAL = 0xAAAAAA;
    private static final int NAME_BOX_H = 22;
    private static final int NAME_BOX_W_PADDING = 12;
    private static final int NAME_BOX_X_OFFSET = 15;
    private static final int NAME_BOX_OVERLAP = 12;
    private static final int MAIN_BOX_H = 90;
    private static final int PADDING_H = 15;
    private static final int PADDING_V = 12;
    private static final int AVATAR_AREA_W = 75;
    private static final int AVATAR_SIZE_RENDER = 70;
    private static final int LINE_HEIGHT = 14;

    public GuiLevelUp(String avatarId) {
        super(Component.translatable("gui.blacksouls.levelup.title"));
        this.npcAvatarId = avatarId;
    }

    public GuiLevelUp() {
        this("noden");
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null && this.minecraft.player != null) {
            this.playerName = this.minecraft.player.getDisplayName().getString();
            this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> this.currentSouls = stats.souls);
        }
        this.avatarTex = new ResourceLocation(BlackSouls.MODID, "textures/gui/avatars/" + npcAvatarId + ".png");
        this.dialogueState = 0;
        this.currentDigitIndex = 0;
        for (int i = 0; i < 6; i++) {
            this.digits[i] = 0;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        if (dialogueState == 1) {
            int boxW = 160;
            int boxH = 50;
            int boxX = (this.width - boxW) / 2;
            int boxY = (this.height - boxH) / 2;

            BSGuiUtils.drawRMWindow(guiGraphics, boxX, boxY, boxW, boxH);

            int startX = boxX + 25;
            int startY = boxY + 20;
            int spacing = 20;

            for (int i = 0; i < 6; i++) {
                int digitX = startX + (i * spacing);
                if (i == currentDigitIndex) {
                    int alpha = (int) (60 + 40 * Math.sin(System.currentTimeMillis() / 150.0));
                    int cursorColor = (alpha << 24) | 0xFFFFFF;
                    guiGraphics.fill(digitX - 4, startY - 4, digitX + 12, startY + 12, cursorColor);
                    guiGraphics.renderOutline(digitX - 4, startY - 4, 16, 16, 0x88FFFFFF);
                }
                guiGraphics.drawString(font, String.valueOf(digits[i]), digitX, startY, COLOR_TEXT_HIGHLIGHT, false);
            }
        }
        else {
            int mainBoxTop = this.height - MAIN_BOX_H;
            int mainBoxWidth = this.width;

            BSGuiUtils.drawRMWindow(guiGraphics, 0, mainBoxTop, mainBoxWidth, MAIN_BOX_H);

            Component nameComp = Component.translatable(KEY_NPC_NAME);
            int nameTextWidth = font.width(nameComp);
            int nameBoxWidth = nameTextWidth + NAME_BOX_W_PADDING * 2;
            int nameBoxLeft = NAME_BOX_X_OFFSET;
            int nameBoxTop = mainBoxTop - (NAME_BOX_H - NAME_BOX_OVERLAP);

            BSGuiUtils.drawRMWindow(guiGraphics, nameBoxLeft, nameBoxTop, nameBoxWidth, NAME_BOX_H);
            guiGraphics.drawString(font, nameComp, nameBoxLeft + NAME_BOX_W_PADDING, nameBoxTop + 7, COLOR_TEXT_HIGHLIGHT, false);

            int contentLeft = PADDING_H;
            int contentTop = mainBoxTop + PADDING_V;

            if (npcAvatarId != null && !npcAvatarId.isEmpty()) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                guiGraphics.blit(this.avatarTex, contentLeft, contentTop, AVATAR_SIZE_RENDER, AVATAR_SIZE_RENDER, 0, 0, 96, 96, 96, 96);
                RenderSystem.disableBlend();
            }

            String dialogText = switch (dialogueState) {
                case 0 -> I18n.get(KEY_DIALOG_WELCOME, playerName);
                case 2 -> I18n.get(KEY_DIALOG_CONFIRM, playerName);
                case 3 -> I18n.get(KEY_DIALOG_CANCEL);
                default -> "";
            };

            int textAreaLeft = contentLeft + AVATAR_AREA_W + 8;
            int wrapWidth = this.width - AVATAR_AREA_W - (PADDING_H * 2) - 15;

            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(dialogText), wrapWidth);
            for (int i = 0; i < lines.size(); i++) {
                guiGraphics.drawString(font, lines.get(i), textAreaLeft, contentTop + (i * LINE_HEIGHT), COLOR_TEXT_HIGHLIGHT, false);
            }

            long time = net.minecraft.Util.getMillis();
            if ((time / 300) % 2 == 0) {
                guiGraphics.drawString(font, "▼", this.width - PADDING_H - 12, mainBoxTop + MAIN_BOX_H - 16, COLOR_TEXT_NORMAL, false);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && dialogueState != 1) {
            advanceState();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (dialogueState != 1) {
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER || keyCode == InputConstants.KEY_SPACE) {
                advanceState();
                return true;
            }
            if (keyCode == InputConstants.KEY_ESCAPE) {
                if (dialogueState == 0) {
                    dialogueState = 3;
                    playCursorSound();
                } else {
                    this.onClose();
                }
                return true;
            }
        }
        else {
            if (keyCode == InputConstants.KEY_RIGHT) {
                currentDigitIndex = (currentDigitIndex + 1) % 6;
                playCursorSound(); return true;
            }
            if (keyCode == InputConstants.KEY_LEFT) {
                currentDigitIndex = (currentDigitIndex + 5) % 6;
                playCursorSound(); return true;
            }
            if (keyCode == InputConstants.KEY_UP) {
                digits[currentDigitIndex] = (digits[currentDigitIndex] + 1) % 10;
                playCursorSound(); return true;
            }
            if (keyCode == InputConstants.KEY_DOWN) {
                digits[currentDigitIndex] = (digits[currentDigitIndex] + 9) % 10;
                playCursorSound(); return true;
            }

            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                long inputAmount = calculateAmount();
                if (inputAmount == 0) {
                    dialogueState = 3;
                    playCursorSound();
                } else if (inputAmount > currentSouls) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.level().playSound(null, this.minecraft.player.blockPosition(), BlackSouls.CURSOR1_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F);
                    }
                } else {
                    NetworkHandler.INSTANCE.sendToServer(new PacketConvertSouls(inputAmount));
                    dialogueState = 2;
                    playCursorSound();
                }
                return true;
            }

            if (keyCode == InputConstants.KEY_ESCAPE) {
                dialogueState = 3;
                playCursorSound();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void advanceState() {
        if (dialogueState == 0) {
            dialogueState = 1;
            playCursorSound();
        } else if (dialogueState == 2 || dialogueState == 3) {
            this.onClose();
        }
    }

    private long calculateAmount() {
        long amount = 0;
        long multiplier = 100000;
        for (int i = 0; i < 6; i++) {
            amount += digits[i] * multiplier;
            multiplier /= 10;
        }
        return amount;
    }

    private void playCursorSound() {
        if (BlackSouls.CURSOR1_EVENT != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
