package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketKillDialogueNPC;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSetCovenant;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundNodenRewardPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuiDialogueEnhanced extends Screen {
    private final String npcNameKey;
    private final String npcAvatarId;
    private final boolean showOptionsAfterDialogue;
    private final int entityId;
    private final int covenantLevel;
    private String[] currentDialogueKeys;
    private int currentLineIndex = 0;
    private int charDisplayIndex = 0;
    private int tickCounter = 0;
    private boolean showOptionsMode = false;
    private boolean isKillingMode = false;
    private boolean isCovenantFirstMode = false;
    private boolean isCovenantSecondMode = false;
    private boolean isCovenantThirdMode = false;
    private boolean isCovenantFourthMode = false;
    private boolean isCovenantFifthMode = false; 
    private boolean isExitMode = false;
    private boolean isSeekServiceMode = false;
    private boolean isKissMode = false;
    private enum DialogueOption {
        UPGRADE("gui.blacksouls.dialogue.option.upgrade", 0xFFFFFF),
        BUY_SOULS("gui.blacksouls.dialogue.option.buy_colored_souls", 0xFFFFFF),
        COVENANT("gui.blacksouls.dialogue.option.covenant", 0xFFFFFF),
        KILL("gui.blacksouls.dialogue.option.kill", 0xFFFF0000),
        OFFER_SOULS("gui.blacksouls.dialogue.option.offer_souls", 0xFFFFFF),
        MAKE_CLOCK("gui.blacksouls.dialogue.option.make_clock", 0xFFFFFF),
        KISS("gui.blacksouls.dialogue.option.kiss", 0xFFFFFF),
        SEEK_SERVICE("gui.blacksouls.dialogue.option.seek_service", 0xFFFFFF), 
        FALL_IN_LOVE("gui.blacksouls.dialogue.option.fall_in_love", 0xFFFFFF), 
        EXIT("gui.blacksouls.dialogue.option.exit", 0xFFFFFF);
        final String translationKey;
        final int textColor;
        DialogueOption(String translationKey, int textColor) {
            this.translationKey = translationKey;
            this.textColor = textColor;
        }
    }
    private final DialogueOption[] mainOptions = {DialogueOption.UPGRADE, DialogueOption.BUY_SOULS, DialogueOption.COVENANT, DialogueOption.KILL, DialogueOption.EXIT};
    private final DialogueOption[] covenantOptions = {DialogueOption.OFFER_SOULS, DialogueOption.EXIT};
    private final DialogueOption[] covenantThirdOptions = {DialogueOption.OFFER_SOULS, DialogueOption.MAKE_CLOCK, DialogueOption.KISS, DialogueOption.EXIT};
    private final DialogueOption[] covenantFourthOptions = {DialogueOption.OFFER_SOULS, DialogueOption.MAKE_CLOCK, DialogueOption.KISS, DialogueOption.SEEK_SERVICE, DialogueOption.EXIT};
    private final DialogueOption[] covenantFifthOptions = {DialogueOption.OFFER_SOULS, DialogueOption.MAKE_CLOCK, DialogueOption.KISS, DialogueOption.SEEK_SERVICE, DialogueOption.FALL_IN_LOVE, DialogueOption.EXIT};
    private DialogueOption[] activeOptions;
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
    private static final int OPTION_BOX_WIDTH = 100;
    private static final int OPTION_LINE_HEIGHT = 16;
    private static final int OPTION_BOX_PADDING_V = 10;

    public GuiDialogueEnhanced(String npcNameKey, String npcAvatarId, String[] dialogueKeys, boolean showOptionsAfterDialogue, int entityId, int covenantLevel) {
        super(Component.translatable(npcNameKey));
        this.npcNameKey = npcNameKey;
        this.npcAvatarId = npcAvatarId;
        this.currentDialogueKeys = dialogueKeys;
        this.showOptionsAfterDialogue = showOptionsAfterDialogue;
        this.entityId = entityId;
        this.covenantLevel = covenantLevel;
        this.activeOptions = mainOptions;
    }

    private String getCurrentLineText() {
        if (currentLineIndex >= currentDialogueKeys.length) return "";
        String key = currentDialogueKeys[currentLineIndex];
        if (key.equals("kill_response")) return I18n.get("gui.blacksouls.dialogue.kill_response");
        if (!I18n.exists(key)) return "Translation missing: " + key;
        Player player = Minecraft.getInstance().player;
        String playerName = player != null ? player.getName().getString() : "master";
        return I18n.get(key, playerName);
    }

    @Override
    public void tick() {
        super.tick();
        if (showOptionsMode) return;
        if (currentLineIndex >= currentDialogueKeys.length) return;

        String fullText = getCurrentLineText();
        int speed = Screen.hasControlDown() ? 8 : 1;

        if (charDisplayIndex < fullText.length()) {
            tickCounter += speed;
            if (tickCounter >= 2) {
                charDisplayIndex++;
                tickCounter = 0;
                if (!Screen.hasControlDown()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.2F));
                }
            }
        } else if (Screen.hasControlDown() && tickCounter++ > 15) {
            advanceDialogue(fullText);
            tickCounter = 0;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int mainBoxTop = this.height - MAIN_BOX_H;
        int mainBoxWidth = this.width;

        Component nameComp = Component.translatable(npcNameKey);
        int nameTextWidth = font.width(nameComp);
        int nameBoxWidth = nameTextWidth + NAME_BOX_W_PADDING * 2;
        int nameBoxLeft = NAME_BOX_X_OFFSET;
        int nameBoxTop = mainBoxTop - (NAME_BOX_H - NAME_BOX_OVERLAP);

        int optionBoxHeight = (activeOptions.length * OPTION_LINE_HEIGHT) + (OPTION_BOX_PADDING_V * 2);
        int optionBoxLeft = this.width - OPTION_BOX_WIDTH;
        int optionBoxTop = mainBoxTop - optionBoxHeight;

        BSGuiUtils.drawRMWindow(guiGraphics, 0, mainBoxTop, mainBoxWidth, MAIN_BOX_H);

        if (showOptionsMode) {
            BSGuiUtils.drawRMWindow(guiGraphics, optionBoxLeft, optionBoxTop, OPTION_BOX_WIDTH, optionBoxHeight);
            int textStartX = optionBoxLeft + 10;
            int textStartY = optionBoxTop + OPTION_BOX_PADDING_V;
            for (int i = 0; i < activeOptions.length; i++) {
                int currentY = textStartY + (i * OPTION_LINE_HEIGHT);
                boolean isHovered = mouseX >= optionBoxLeft + 5 && mouseX <= optionBoxLeft + OPTION_BOX_WIDTH - 5 && mouseY >= currentY && mouseY < currentY + OPTION_LINE_HEIGHT;
                boolean isKillOption = activeOptions[i] == DialogueOption.KILL;

                if (isHovered) guiGraphics.fill(optionBoxLeft + 5, currentY + 1, optionBoxLeft + OPTION_BOX_WIDTH - 5, currentY + OPTION_LINE_HEIGHT - 1, 0x66FFFFFF);

                int color = activeOptions[i].textColor;
                if (!isKillOption && isHovered) color = 0xFFFFFF;

                guiGraphics.drawString(font, Component.translatable(activeOptions[i].translationKey), textStartX, currentY + 3, color, false);
            }
        }

        BSGuiUtils.drawRMWindow(guiGraphics, nameBoxLeft, nameBoxTop, nameBoxWidth, NAME_BOX_H);
        guiGraphics.drawString(font, nameComp, nameBoxLeft + NAME_BOX_W_PADDING, nameBoxTop + 7, 0xFFFFFF, false);

        int contentLeft = PADDING_H;
        int contentTop = mainBoxTop + PADDING_V;

        if (npcAvatarId != null && !npcAvatarId.isEmpty()) {
            ResourceLocation avatarTex = new ResourceLocation(BlackSouls.MODID, "textures/gui/avatars/" + npcAvatarId + ".png");
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(avatarTex, contentLeft, contentTop, AVATAR_SIZE_RENDER, AVATAR_SIZE_RENDER, 0, 0, 96, 96, 96, 96);
            RenderSystem.disableBlend();
        }

        int textAreaLeft = contentLeft + AVATAR_AREA_W + 8;

        if (currentLineIndex < currentDialogueKeys.length) {
            String fullText = getCurrentLineText();
            int displayLen = showOptionsMode ? fullText.length() : Math.min(charDisplayIndex, fullText.length());
            String currentText = fullText.substring(0, displayLen);

            int wrapWidth = this.width - AVATAR_AREA_W - (PADDING_H * 2) - 15;
            List<FormattedCharSequence> wrappedLines = font.split(Component.literal(currentText), wrapWidth);

            for (int i = 0; i < wrappedLines.size(); i++) {
                guiGraphics.drawString(font, wrappedLines.get(i), textAreaLeft, contentTop + (i * LINE_HEIGHT), 0xFFFFFF, false);
            }

            if (charDisplayIndex >= fullText.length() && !showOptionsMode) {
                long time = net.minecraft.Util.getMillis();
                if ((time / 300) % 2 == 0) {
                    guiGraphics.drawString(font, "▼", this.width - PADDING_H - 12, mainBoxTop + MAIN_BOX_H - 16, 0xAAAAAA, false);
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (showOptionsMode) {
                int mainBoxTop = this.height - MAIN_BOX_H;
                int optionBoxHeight = (activeOptions.length * OPTION_LINE_HEIGHT) + (OPTION_BOX_PADDING_V * 2);
                int optionBoxLeft = this.width - OPTION_BOX_WIDTH;
                int optionBoxTop = mainBoxTop - optionBoxHeight;
                int textStartY = optionBoxTop + OPTION_BOX_PADDING_V;

                for (int i = 0; i < activeOptions.length; i++) {
                    int currentY = textStartY + (i * OPTION_LINE_HEIGHT);
                    if (mouseX >= optionBoxLeft + 5 && mouseX <= optionBoxLeft + OPTION_BOX_WIDTH - 5 && mouseY >= currentY && mouseY < currentY + OPTION_LINE_HEIGHT) {
                        if (BlackSouls.CURSOR1_EVENT != null) {
                            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
                        }
                        handleOptionAction(activeOptions[i]);
                        return true;
                    }
                }
                return true;
            }

            String fullText = getCurrentLineText();
            advanceDialogue(fullText);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleOptionAction(DialogueOption option) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        switch (option) {
            case UPGRADE:
                this.minecraft.setScreen(new GuiLevelUp(this.npcAvatarId));
                break;
            case BUY_SOULS:
                this.minecraft.setScreen(new GuiUniversalShop(GuiUniversalShop.ShopType.COLORED_SOULS));
                break;
            case COVENANT:
                this.showOptionsMode = false;
                this.charDisplayIndex = 0;
                this.currentLineIndex = 0;
                this.tickCounter = 0;

                if (covenantLevel == -1) {
                    this.isCovenantFirstMode = true;
                    this.currentDialogueKeys = new String[] { "gui.blacksouls.dialogue.covenant.first_1", "gui.blacksouls.dialogue.covenant.first_2" };
                } else if (covenantLevel == 0) {
                    this.isCovenantSecondMode = true;
                    this.currentDialogueKeys = new String[] { "gui.blacksouls.dialogue.covenant.second_1", "gui.blacksouls.dialogue.covenant.second_2" };
                } else if (covenantLevel == 1) {
                    this.isCovenantThirdMode = true;
                    this.currentDialogueKeys = new String[] { "gui.blacksouls.dialogue.covenant.third_1" };
                } else if (covenantLevel == 2) {
                    this.isCovenantFourthMode = true;
                    this.currentDialogueKeys = new String[] { "gui.blacksouls.dialogue.covenant.fourth_1", "gui.blacksouls.dialogue.covenant.fourth_2" };
                } else if (covenantLevel >= 3) {
                    this.isCovenantFifthMode = true;
                    this.currentDialogueKeys = new String[] { "gui.blacksouls.dialogue.covenant.fifth_1", "gui.blacksouls.dialogue.covenant.fifth_2" };
                }
                break;
            case OFFER_SOULS:
                if (covenantLevel >= 3) {
                    this.minecraft.setScreen(new GuiCovenantMaxBanner());
                    break;
                }

                this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                    long cost = 1000;
                    if (covenantLevel == 1) cost = 3000;
                    if (covenantLevel == 2) cost = 7000;

                    this.minecraft.setScreen(new GuiCovenantUpgradeConfirm(npcNameKey, npcAvatarId, entityId, covenantLevel, stats.souls, cost));
                });
                break;
            case MAKE_CLOCK:
                this.minecraft.setScreen(new GuiUniversalShop(GuiUniversalShop.ShopType.CLOCK_MAKER));
                break;
            case KISS:
                this.showOptionsMode = false;
                this.isKissMode = true;

                if (this.covenantLevel == 1) {
                    this.currentDialogueKeys = new String[] {
                            "gui.blacksouls.dialogue.kiss_1"
                    };
                } else if (this.covenantLevel == 2) {
                    this.currentDialogueKeys = new String[] {
                            "gui.blacksouls.dialogue.kiss_2"
                    };
                } else if (this.covenantLevel >= 3) {
                    this.currentDialogueKeys = new String[] {
                            "gui.blacksouls.dialogue.kiss_3"
                    };
                } else {
                    this.currentDialogueKeys = new String[] { "gui.blacksouls.dialogue.kiss_fallback" };
                }

                this.charDisplayIndex = 0;
                this.currentLineIndex = 0;
                this.tickCounter = 0;
                break;
            case SEEK_SERVICE:
                this.showOptionsMode = false;
                this.isSeekServiceMode = true;

                if (this.covenantLevel == 2) {
                    this.currentDialogueKeys = new String[] {
                            "gui.blacksouls.dialogue.seek_service_1"
                    };
                } else if (this.covenantLevel >= 3) {
                    this.currentDialogueKeys = new String[] {
                            "gui.blacksouls.dialogue.seek_service_2"
                    };
                } else {
                    
                    this.currentDialogueKeys = new String[] { "gui.blacksouls.dialogue.seek_service_fallback" };
                }

                this.charDisplayIndex = 0;
                this.currentLineIndex = 0;
                this.tickCounter = 0;
                break;
            case FALL_IN_LOVE:
                break;
            case KILL:
                this.showOptionsMode = false;
                this.isKillingMode = true;
                this.currentDialogueKeys = new String[] { "kill_response" };
                this.charDisplayIndex = 0;
                this.currentLineIndex = 0;
                this.tickCounter = 0;
                break;
            case EXIT:
                this.showOptionsMode = false;
                this.isExitMode = true; 
                this.currentDialogueKeys = new String[] { "gui.blacksouls.dialogue.exit_message" };
                this.charDisplayIndex = 0;
                this.currentLineIndex = 0;
                this.tickCounter = 0;
                break;
        }
    }

    private void advanceDialogue(String fullText) {
        if (charDisplayIndex < fullText.length()) {
            charDisplayIndex = fullText.length();
        } else {
            if (isKillingMode) {
                if (BlackSouls.GUCHA004A_EVENT != null) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.GUCHA004A_EVENT.get(), 1.0F, 1.0F));
                }
                NetworkHandler.sendToServer(new PacketKillDialogueNPC(this.entityId));
                if (this.npcAvatarId != null && this.npcAvatarId.contains("noden")) {
                    NetworkHandler.sendToServer(new PacketSetCovenant("noden", -2));
                }
                this.onClose();
                return;
            }

            boolean isLastLine = (currentLineIndex == currentDialogueKeys.length - 1);

            if (isLastLine) {
                if (isExitMode) {
                    this.onClose();
                } else if (isKissMode) {
                    this.isKissMode = false;
                    NetworkHandler.sendToServer(new ServerboundNodenRewardPacket(ServerboundNodenRewardPacket.Type.KISS, this.entityId));
                    this.onClose();
                } else if (isSeekServiceMode) {
                    this.isSeekServiceMode = false;
                    NetworkHandler.sendToServer(new ServerboundNodenRewardPacket(ServerboundNodenRewardPacket.Type.SEEK_SERVICE, this.entityId));
                    this.onClose();
                } else if (isCovenantFirstMode) {
                    NetworkHandler.sendToServer(new PacketSetCovenant("noden", 0));
                    String npcTranslatedName = I18n.get(npcNameKey);
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new GuiCovenantBanner(npcTranslatedName));
                    }
                } else if (isCovenantSecondMode) {
                    this.activeOptions = covenantOptions;
                    this.showOptionsMode = true;
                } else if (isCovenantThirdMode) {
                    this.activeOptions = covenantThirdOptions;
                    this.showOptionsMode = true;
                } else if (isCovenantFourthMode) {
                    this.activeOptions = covenantFourthOptions;
                    this.showOptionsMode = true;
                } else if (isCovenantFifthMode) {
                    this.activeOptions = covenantFifthOptions;
                    this.showOptionsMode = true;
                } else if (showOptionsAfterDialogue) {
                    this.activeOptions = mainOptions;
                    this.showOptionsMode = true;
                } else {
                    this.onClose();
                }
            } else {
                currentLineIndex++;
                charDisplayIndex = 0;
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
