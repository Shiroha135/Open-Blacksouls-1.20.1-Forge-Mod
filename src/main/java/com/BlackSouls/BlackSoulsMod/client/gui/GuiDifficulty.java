package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.BonfireEffectRenderer;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSetDifficulty;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundSimpleActionPacket;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class GuiDifficulty extends net.minecraft.client.gui.screens.Screen {
    private static final int MESSAGE_WIDTH = 520;
    private static final int MESSAGE_HEIGHT = 104;
    private Stage stage = Stage.INTRO;
    private int revealedCodePoints;
    private int revealTick;
    private boolean widgetsShown;
    private int messageLeft;
    private int messageTop;
    private int messageWidth;

    public GuiDifficulty() {
        super(Component.translatable("gui.blacksouls.difficulty.title"));
        NetworkHandler.INSTANCE.sendToServer(new ServerboundSimpleActionPacket(
                ServerboundSimpleActionPacket.Action.VISIT_DIFFICULTY_STATUE));
    }

    @Override
    protected void init() {
        messageWidth = Math.min(MESSAGE_WIDTH, width - 24);
        messageLeft = (width - messageWidth) / 2;
        messageTop = height - MESSAGE_HEIGHT - 18;
        clearWidgets();
        widgetsShown = false;
        if (stage == Stage.LEVELS) {
            addDifficultyButtons();
        } else if (stage == Stage.CONFIRM && isTextComplete()) {
            addConfirmationButtons();
        }
    }

    @Override
    public void tick() {
        if (stage == Stage.LEVELS || isTextComplete()) {
            if (stage == Stage.CONFIRM && !widgetsShown) {
                addConfirmationButtons();
            }
            return;
        }
        revealTick++;
        if (revealTick >= 1) {
            revealTick = 0;
            revealedCodePoints++;
            if (stage == Stage.CONFIRM && isTextComplete()) {
                addConfirmationButtons();
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        BSGuiUtils.drawRMWindow(guiGraphics, messageLeft, messageTop, messageWidth, MESSAGE_HEIGHT);

        String text = visibleText();
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), messageWidth - 32);
        int y = messageTop + 17;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, messageLeft + 18, y, 0xFFFFFFFF, true);
            y += font.lineHeight + 2;
        }

        if (stage == Stage.INTRO && isTextComplete()) {
            int bob = ((System.currentTimeMillis() / 250L) & 1L) == 0L ? 0 : 2;
            guiGraphics.drawCenteredString(font, "▼", width / 2, messageTop + MESSAGE_HEIGHT - 18 + bob, 0xFFFFFFFF);
        }

        if (stage == Stage.LEVELS) {
            int menuWidth = width < 360 || height < 360 ? 244 : 132;
            int menuHeight = width < 360 || height < 360 ? 130 : 232;
            int menuLeft = (width - menuWidth) / 2;
            int menuTop = Math.max(8, messageTop - menuHeight - 8);
            BSGuiUtils.drawRMWindow(guiGraphics, menuLeft, menuTop, menuWidth, menuHeight);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!isTextComplete() && stage != Stage.LEVELS) {
            revealAll();
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (stage == Stage.INTRO) {
            setStage(Stage.CONFIRM);
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (!isTextComplete() && stage != Stage.LEVELS) {
                revealAll();
            } else if (stage == Stage.INTRO) {
                setStage(Stage.CONFIRM);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addConfirmationButtons() {
        if (widgetsShown) {
            return;
        }
        widgetsShown = true;
        int x = messageLeft + messageWidth - 92;
        addRenderableWidget(new BSGhostButton(x, messageTop + 14, 70, 20,
                Component.translatable("gui.blacksouls.difficulty.yes"), button -> {
            playCursor();
            setStage(Stage.LEVELS);
        }));
        addRenderableWidget(new BSGhostButton(x, messageTop + 40, 70, 20,
                Component.translatable("gui.blacksouls.difficulty.no"), button -> {
            playCursor();
            onClose();
        }));
    }

    private void addDifficultyButtons() {
        clearWidgets();
        widgetsShown = true;
        boolean compact = width < 360 || height < 360;
        int menuWidth = compact ? 244 : 132;
        int menuHeight = compact ? 130 : 232;
        int menuLeft = (width - menuWidth) / 2;
        int menuTop = Math.max(8, messageTop - menuHeight - 8);
        for (int level = 0; level <= 9; level++) {
            int col = compact ? level / 5 : 0;
            int row = compact ? level % 5 : level;
            int x = menuLeft + 11 + col * 116;
            int y = menuTop + 11 + row * 22;
            int selected = level;
            addRenderableWidget(new BSGhostButton(x, y, 110, 20,
                    Component.translatable("gui.blacksouls.difficulty.lv", level), button -> selectDifficulty(selected)));
        }
    }

    private void selectDifficulty(int difficulty) {
        playCursor();
        NetworkHandler.INSTANCE.sendToServer(new PacketSetDifficulty(difficulty));
        DifficultyManager.currentDifficulty = difficulty;
        BonfireEffectRenderer.whiteFlashTicks = 20;
        onClose();
    }

    private void setStage(Stage newStage) {
        stage = newStage;
        revealedCodePoints = 0;
        revealTick = 0;
        clearWidgets();
        widgetsShown = false;
        if (stage == Stage.LEVELS) {
            addDifficultyButtons();
        }
    }

    private String fullText() {
        if (stage == Stage.INTRO) {
            String deaths = Component.translatable("gui.blacksouls.difficulty.deaths", DifficultyManager.deathCount).getString();
            if (DifficultyManager.loopCount > 0) {
                return deaths + "\n\n\n" + Component.translatable(
                        "gui.blacksouls.difficulty.loops", DifficultyManager.loopCount).getString();
            }
            return deaths;
        }
        return Component.translatable("gui.blacksouls.difficulty.ask").getString()
                + "\n" + Component.translatable("gui.blacksouls.difficulty.explain").getString()
                + "\n" + Component.translatable(
                        "gui.blacksouls.difficulty.current_original", DifficultyManager.currentDifficulty).getString();
    }

    private String visibleText() {
        String full = fullText();
        if (stage == Stage.LEVELS || isTextComplete()) {
            return full;
        }
        int count = Math.min(revealedCodePoints, full.codePointCount(0, full.length()));
        return full.substring(0, full.offsetByCodePoints(0, count));
    }

    private boolean isTextComplete() {
        String full = fullText();
        return revealedCodePoints >= full.codePointCount(0, full.length());
    }

    private void revealAll() {
        String full = fullText();
        revealedCodePoints = full.codePointCount(0, full.length());
        if (stage == Stage.CONFIRM) {
            addConfirmationButtons();
        }
    }

    private void playCursor() {
        if (minecraft != null && BlackSouls.CURSOR1_EVENT != null && BlackSouls.CURSOR1_EVENT.isPresent()) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
        }
    }

    private enum Stage {
        INTRO,
        CONFIRM,
        LEVELS
    }
}
