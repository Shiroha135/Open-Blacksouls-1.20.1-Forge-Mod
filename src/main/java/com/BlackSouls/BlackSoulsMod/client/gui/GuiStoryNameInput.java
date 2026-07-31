package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.client.ClientStoryName;
import com.BlackSouls.BlackSoulsMod.util.StoryNameData;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class GuiStoryNameInput extends Screen {
    private static final int INPUT_MAX_WIDTH = 360;
    private static final int INPUT_HEIGHT = 64;
    private static final int KEYBOARD_MAX_WIDTH = 480;
    private static final int KEYBOARD_MAX_HEIGHT = 220;
    private static final int KEYBOARD_MIN_HEIGHT = 140;
    private static final int MODE_WIDTH = 96;
    private static final int GRID_COLUMNS = 10;
    private static final List<String> HIRAGANA = characters(
        "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん" +
        "がぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽぁぃぅぇぉゃゅょっー"
    );
    private static final List<String> KATAKANA = characters(
        "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン" +
        "ガギグゲゴザジズゼゾダヂヅデドバビブベボパピプペポァィゥェォャュョッー"
    );
    private static final List<String> ALPHANUMERIC = characters(
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    );
    private static final List<String> SYMBOLS = characters("0123456789+-*/=!?.,:;_~@#$%&()[]{}<>'");
    private static final String[] MODE_KEYS = {
        "gui.blacksouls.story_name.hiragana",
        "gui.blacksouls.story_name.katakana",
        "gui.blacksouls.story_name.alphanumeric",
        "gui.blacksouls.story_name.symbols",
        "gui.blacksouls.story_name.backspace",
        "gui.blacksouls.story_name.done"
    };
    private String name;
    private int page;
    private int selectedCharacter;

    public GuiStoryNameInput(String initialName) {
        super(Component.translatable("gui.blacksouls.story_name.input"));
        this.name = StoryNameData.normalize(initialName);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF000000);
        Layout layout = layout();
        BSGuiUtils.drawRMWindow(graphics, layout.inputLeft, layout.inputTop, layout.inputWidth, INPUT_HEIGHT);
        BSGuiUtils.drawRMWindow(graphics, layout.keyboardLeft, layout.keyboardTop, layout.modeWidth, layout.keyboardHeight);
        BSGuiUtils.drawRMWindow(
            graphics,
            layout.keyboardLeft + layout.modeWidth,
            layout.keyboardTop,
            layout.keyboardWidth - layout.modeWidth,
            layout.keyboardHeight
        );
        renderName(graphics, layout);
        renderModes(graphics, mouseX, mouseY, layout);
        renderCharacters(graphics, mouseX, mouseY, layout);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderName(GuiGraphics graphics, Layout layout) {
        String caret = (net.minecraft.Util.getMillis() / 400L) % 2L == 0L ? "_" : "";
        String display = name + caret;
        float scale = 1.5F;
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, layout.inputTop + INPUT_HEIGHT / 2.0F - 6.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, display, -font.width(display) / 2, 0, 0xFFFFFFFF, false);
        graphics.pose().popPose();
    }

    private void renderModes(GuiGraphics graphics, int mouseX, int mouseY, Layout layout) {
        int rowHeight = Math.max(18, (layout.keyboardHeight - 16) / MODE_KEYS.length);
        int hovered = modeAt(mouseX, mouseY, layout, rowHeight);
        for (int index = 0; index < MODE_KEYS.length; index++) {
            int y = layout.keyboardTop + 8 + index * rowHeight;
            if (index == page || index == hovered) {
                graphics.fill(
                    layout.keyboardLeft + 7,
                    y,
                    layout.keyboardLeft + layout.modeWidth - 7,
                    y + rowHeight - 2,
                    0x66FFFFFF
                );
            }
            graphics.drawCenteredString(
                font,
                Component.translatable(MODE_KEYS[index]),
                layout.keyboardLeft + layout.modeWidth / 2,
                y + (rowHeight - font.lineHeight) / 2,
                index >= 4 ? 0xFF77AAFF : 0xFFFFFFFF
            );
        }
    }

    private void renderCharacters(GuiGraphics graphics, int mouseX, int mouseY, Layout layout) {
        List<String> characters = activeCharacters();
        int rows = Math.max(1, (characters.size() + GRID_COLUMNS - 1) / GRID_COLUMNS);
        int areaLeft = layout.keyboardLeft + layout.modeWidth + 8;
        int areaTop = layout.keyboardTop + 8;
        int areaWidth = layout.keyboardWidth - layout.modeWidth - 16;
        int areaHeight = layout.keyboardHeight - 16;
        int cellWidth = Math.max(12, areaWidth / GRID_COLUMNS);
        int cellHeight = Math.max(14, Math.min(24, areaHeight / rows));
        int hovered = characterAt(mouseX, mouseY, layout, characters.size(), cellWidth, cellHeight);
        if (hovered >= 0) {
            selectedCharacter = hovered;
        }
        for (int index = 0; index < characters.size(); index++) {
            int column = index % GRID_COLUMNS;
            int row = index / GRID_COLUMNS;
            int x = areaLeft + column * cellWidth;
            int y = areaTop + row * cellHeight;
            if (index == selectedCharacter) {
                graphics.fill(x, y, x + cellWidth - 1, y + cellHeight - 1, 0x66FFFFFF);
            }
            graphics.drawCenteredString(
                font,
                characters.get(index),
                x + cellWidth / 2,
                y + (cellHeight - font.lineHeight) / 2,
                0xFFFFFFFF
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        Layout layout = layout();
        int modeRowHeight = Math.max(18, (layout.keyboardHeight - 16) / MODE_KEYS.length);
        int clickedMode = modeAt(mouseX, mouseY, layout, modeRowHeight);
        if (clickedMode >= 0) {
            if (clickedMode < 4) {
                page = clickedMode;
                selectedCharacter = 0;
                ClientStoryName.playCursor();
            } else if (clickedMode == 4) {
                backspace();
            } else {
                finish();
            }
            return true;
        }

        List<String> characters = activeCharacters();
        int rows = Math.max(1, (characters.size() + GRID_COLUMNS - 1) / GRID_COLUMNS);
        int areaWidth = layout.keyboardWidth - layout.modeWidth - 16;
        int areaHeight = layout.keyboardHeight - 16;
        int cellWidth = Math.max(12, areaWidth / GRID_COLUMNS);
        int cellHeight = Math.max(14, Math.min(24, areaHeight / rows));
        int clickedCharacter = characterAt(
            mouseX,
            mouseY,
            layout,
            characters.size(),
            cellWidth,
            cellHeight
        );
        if (clickedCharacter >= 0) {
            selectedCharacter = clickedCharacter;
            append(characters.get(clickedCharacter));
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (SharedConstants.isAllowedChatCharacter(codePoint)) {
            append(String.valueOf(codePoint));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            backspace();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            finish();
            return true;
        }
        List<String> characters = activeCharacters();
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            selectedCharacter = Math.floorMod(selectedCharacter - 1, characters.size());
            ClientStoryName.playCursor();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            selectedCharacter = Math.floorMod(selectedCharacter + 1, characters.size());
            ClientStoryName.playCursor();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            selectedCharacter = Math.floorMod(selectedCharacter - GRID_COLUMNS, characters.size());
            ClientStoryName.playCursor();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            selectedCharacter = Math.floorMod(selectedCharacter + GRID_COLUMNS, characters.size());
            ClientStoryName.playCursor();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            append(characters.get(selectedCharacter));
            return true;
        }
        return true;
    }

    private void append(String value) {
        if (name.codePointCount(0, name.length()) >= StoryNameData.MAX_LENGTH) {
            return;
        }
        name += value;
        ClientStoryName.playCursor();
    }

    private void backspace() {
        int count = name.codePointCount(0, name.length());
        if (count == 0) {
            return;
        }
        name = name.substring(0, name.offsetByCodePoints(0, count - 1));
        ClientStoryName.playCursor();
    }

    private void finish() {
        String normalized = StoryNameData.normalize(name);
        if (normalized.isEmpty() || minecraft == null) {
            return;
        }
        name = normalized;
        ClientStoryName.playCursor();
        minecraft.setScreen(new GuiStoryNameConfirm(name));
    }

    private int modeAt(double mouseX, double mouseY, Layout layout, int rowHeight) {
        if (mouseX < layout.keyboardLeft + 7
            || mouseX >= layout.keyboardLeft + layout.modeWidth - 7
            || mouseY < layout.keyboardTop + 8
            || mouseY >= layout.keyboardTop + 8 + rowHeight * MODE_KEYS.length) {
            return -1;
        }
        return (int) ((mouseY - layout.keyboardTop - 8) / rowHeight);
    }

    private int characterAt(
        double mouseX,
        double mouseY,
        Layout layout,
        int characterCount,
        int cellWidth,
        int cellHeight
    ) {
        int areaLeft = layout.keyboardLeft + layout.modeWidth + 8;
        int areaTop = layout.keyboardTop + 8;
        if (mouseX < areaLeft || mouseY < areaTop) {
            return -1;
        }
        int column = (int) ((mouseX - areaLeft) / cellWidth);
        int row = (int) ((mouseY - areaTop) / cellHeight);
        if (column < 0 || column >= GRID_COLUMNS || row < 0) {
            return -1;
        }
        int index = row * GRID_COLUMNS + column;
        return index < characterCount ? index : -1;
    }

    private List<String> activeCharacters() {
        return switch (page) {
            case 1 -> KATAKANA;
            case 2 -> ALPHANUMERIC;
            case 3 -> SYMBOLS;
            default -> HIRAGANA;
        };
    }

    private Layout layout() {
        int inputWidth = Math.min(INPUT_MAX_WIDTH, Math.max(180, width - 28));
        int keyboardWidth = Math.min(KEYBOARD_MAX_WIDTH, Math.max(240, width - 28));
        int keyboardHeight = Math.min(
            KEYBOARD_MAX_HEIGHT,
            Math.max(KEYBOARD_MIN_HEIGHT, height - INPUT_HEIGHT - 44)
        );
        int totalHeight = INPUT_HEIGHT + 10 + keyboardHeight;
        int inputTop = Math.max(8, (height - totalHeight) / 2);
        int keyboardTop = inputTop + INPUT_HEIGHT + 10;
        int modeWidth = Math.min(MODE_WIDTH, keyboardWidth / 3);
        return new Layout(
            (width - inputWidth) / 2,
            inputTop,
            inputWidth,
            (width - keyboardWidth) / 2,
            keyboardTop,
            keyboardWidth,
            keyboardHeight,
            modeWidth
        );
    }

    private static List<String> characters(String value) {
        return value.codePoints().mapToObj(Character::toString).toList();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Layout(
        int inputLeft,
        int inputTop,
        int inputWidth,
        int keyboardLeft,
        int keyboardTop,
        int keyboardWidth,
        int keyboardHeight,
        int modeWidth
    ) {
    }
}
