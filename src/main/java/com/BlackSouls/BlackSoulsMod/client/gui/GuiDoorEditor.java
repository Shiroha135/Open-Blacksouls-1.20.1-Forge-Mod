package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundDoorEditorPacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundSaveDoorConfigPacket;
import com.BlackSouls.BlackSoulsMod.util.DoorConfigMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public final class GuiDoorEditor extends Screen {
    private static final int NORMAL_COLOR = 0xE0E0E0;
    private static final int INVALID_COLOR = 0xFF5555;
    private final ClientboundDoorEditorPacket initial;
    private DoorConfigMode mode;
    private boolean consume;
    private boolean eventTriggered;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int leftX;
    private int rightX;
    private int columnWidth;
    private BSGhostButton modeButton;
    private EditBox itemField;
    private BSGhostButton consumeButton;
    private BSGhostButton offhandButton;
    private EditBox eventField;
    private EditBox conditionField;
    private EditBox dimensionField;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private BSGhostButton currentPositionButton;
    private BSGhostButton eventStateButton;

    public GuiDoorEditor(ClientboundDoorEditorPacket initial) {
        super(Component.translatable("gui.blacksouls.door.editor.title"));
        this.initial = initial;
        this.mode = initial.mode();
        this.consume = initial.consume();
        this.eventTriggered = initial.eventTriggered();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(480, width - 24);
        panelHeight = Math.min(220, height - 20);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int gap = 12;
        int padding = 14;
        columnWidth = (panelWidth - padding * 2 - gap) / 2;
        leftX = panelX + padding;
        rightX = leftX + columnWidth + gap;

        modeButton = addRenderableWidget(new BSGhostButton(
                leftX,
                panelY + 36,
                panelWidth - padding * 2,
                20,
                modeText(),
                button -> cycleMode()
        ));

        itemField = editBox(leftX, panelY + 80, columnWidth, initial.requiredItem(), 128);
        consumeButton = addRenderableWidget(new BSGhostButton(
                leftX,
                panelY + 111,
                columnWidth,
                20,
                consumeText(),
                button -> {
                    consume = !consume;
                    button.setMessage(consumeText());
                }
        ));
        offhandButton = addRenderableWidget(new BSGhostButton(
                rightX,
                panelY + 80,
                columnWidth,
                20,
                Component.translatable("gui.blacksouls.door.editor.use_offhand"),
                button -> useOffhandItem()
        ));

        eventField = editBox(leftX, panelY + 80, columnWidth, initial.eventId(), 64);
        conditionField = editBox(leftX, panelY + 116, columnWidth, initial.conditionId(), 64);
        dimensionField = editBox(rightX, panelY + 80, columnWidth, initial.targetDimension(), 128);
        int coordinateGap = 5;
        int coordinateWidth = (columnWidth - coordinateGap * 2) / 3;
        xField = editBox(rightX, panelY + 116, coordinateWidth, number(initial.targetX()), 32);
        yField = editBox(rightX + coordinateWidth + coordinateGap, panelY + 116,
                coordinateWidth, number(initial.targetY()), 32);
        zField = editBox(rightX + (coordinateWidth + coordinateGap) * 2, panelY + 116,
                coordinateWidth, number(initial.targetZ()), 32);
        currentPositionButton = addRenderableWidget(new BSGhostButton(
                rightX,
                panelY + 145,
                (columnWidth - 6) / 2,
                20,
                Component.translatable("gui.blacksouls.door.editor.current_position"),
                button -> useCurrentPosition()
        ));
        eventStateButton = addRenderableWidget(new BSGhostButton(
                rightX + (columnWidth - 6) / 2 + 6,
                panelY + 145,
                (columnWidth - 6) / 2,
                20,
                eventStateText(),
                button -> {
                    eventTriggered = !eventTriggered;
                    button.setMessage(eventStateText());
                }
        ));

        int buttonY = panelY + panelHeight - 28;
        int actionWidth = 88;
        addRenderableWidget(new BSGhostButton(
                width / 2 - actionWidth - 5,
                buttonY,
                actionWidth,
                20,
                Component.translatable("gui.done"),
                button -> save()
        ));
        addRenderableWidget(new BSGhostButton(
                width / 2 + 5,
                buttonY,
                actionWidth,
                20,
                Component.translatable("gui.cancel"),
                button -> onClose()
        ));
        updateVisibility();
    }

    private EditBox editBox(int x, int y, int width, String value, int maxLength) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private void cycleMode() {
        mode = mode.next();
        if (mode == DoorConfigMode.SHORTCUT_GATE) {
            eventField.setValue("Map051 EV008");
            if (conditionField.getValue().isBlank()) {
                conditionField.setValue("Map051 EV033");
            }
        } else if (mode == DoorConfigMode.SHORTCUT_UNLOCK) {
            eventField.setValue("Map051 EV033");
            if (conditionField.getValue().isBlank()) {
                conditionField.setValue("Map051 EV033");
            }
        }
        modeButton.setMessage(modeText());
        updateVisibility();
    }

    private void updateVisibility() {
        boolean story = mode == DoorConfigMode.STORY_LOCK;
        boolean shortcut = mode == DoorConfigMode.SHORTCUT_GATE
                || mode == DoorConfigMode.SHORTCUT_UNLOCK;
        itemField.visible = story;
        consumeButton.visible = mode == DoorConfigMode.NORMAL_LOCK || story;
        offhandButton.visible = story;
        eventField.visible = shortcut;
        conditionField.visible = shortcut;
        dimensionField.visible = shortcut;
        xField.visible = shortcut;
        yField.visible = shortcut;
        zField.visible = shortcut;
        currentPositionButton.visible = shortcut;
        eventStateButton.visible = shortcut;
    }

    private void useOffhandItem() {
        if (minecraft == null || minecraft.player == null || minecraft.player.getOffhandItem().isEmpty()) {
            itemField.setValue("minecraft:air");
            return;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(minecraft.player.getOffhandItem().getItem());
        if (itemId != null) {
            itemField.setValue(itemId.toString());
            itemField.setTextColor(NORMAL_COLOR);
        }
    }

    private void useCurrentPosition() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        dimensionField.setValue(minecraft.player.level().dimension().location().toString());
        xField.setValue(number(minecraft.player.getX()));
        yField.setValue(number(minecraft.player.getY()));
        zField.setValue(number(minecraft.player.getZ()));
        resetColors();
    }

    private void save() {
        resetColors();
        String requiredItem = itemField.getValue().strip();
        String eventId = eventField.getValue().strip();
        String conditionId = conditionField.getValue().strip();
        String targetDimension = dimensionField.getValue().strip();
        double targetX = 0.0D;
        double targetY = 0.0D;
        double targetZ = 0.0D;

        if (mode == DoorConfigMode.STORY_LOCK) {
            ResourceLocation itemId = ResourceLocation.tryParse(requiredItem);
            if (itemId == null
                    || itemId.toString().equals("minecraft:air")
                    || !ForgeRegistries.ITEMS.containsKey(itemId)) {
                itemField.setTextColor(INVALID_COLOR);
                return;
            }
        }
        if (mode == DoorConfigMode.SHORTCUT_GATE || mode == DoorConfigMode.SHORTCUT_UNLOCK) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(targetDimension);
            Double parsedX = parseDouble(xField);
            Double parsedY = parseDouble(yField);
            Double parsedZ = parseDouble(zField);
            if (eventId.isEmpty()) {
                eventField.setTextColor(INVALID_COLOR);
            }
            if (conditionId.isEmpty()) {
                conditionField.setTextColor(INVALID_COLOR);
            }
            if (dimensionId == null) {
                dimensionField.setTextColor(INVALID_COLOR);
            }
            if (eventId.isEmpty() || conditionId.isEmpty() || dimensionId == null
                    || parsedX == null || parsedY == null || parsedZ == null) {
                return;
            }
            targetX = parsedX;
            targetY = parsedY;
            targetZ = parsedZ;
        }

        NetworkHandler.sendToServer(new ServerboundSaveDoorConfigPacket(
                initial.pos(),
                mode,
                requiredItem,
                consume,
                eventId,
                conditionId,
                targetDimension,
                targetX,
                targetY,
                targetZ,
                eventTriggered
        ));
        onClose();
    }

    private Double parseDouble(EditBox field) {
        try {
            double value = Double.parseDouble(field.getValue().strip());
            if (Double.isFinite(value)) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        field.setTextColor(INVALID_COLOR);
        return null;
    }

    private void resetColors() {
        itemField.setTextColor(NORMAL_COLOR);
        eventField.setTextColor(NORMAL_COLOR);
        conditionField.setTextColor(NORMAL_COLOR);
        dimensionField.setTextColor(NORMAL_COLOR);
        xField.setTextColor(NORMAL_COLOR);
        yField.setTextColor(NORMAL_COLOR);
        zField.setTextColor(NORMAL_COLOR);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        BSGuiUtils.drawRMWindow(graphics, panelX, panelY, panelWidth, panelHeight);
        graphics.drawCenteredString(font, title, width / 2, panelY + 11, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.mode"),
                leftX, panelY + 25, 0xC8C8C8, false);

        if (mode == DoorConfigMode.STORY_LOCK) {
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.required_item"),
                    leftX, panelY + 68, 0xC8C8C8, false);
        } else if (mode == DoorConfigMode.NORMAL_LOCK) {
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.master_key"),
                    leftX, panelY + 78, 0xFFFFD27A, false);
        } else if (mode == DoorConfigMode.ANIMATED_GROUP) {
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.animated_status"),
                    leftX, panelY + 78, 0xFFFFD27A, false);
        } else if (mode == DoorConfigMode.NONE) {
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.none_status"),
                    leftX, panelY + 78, 0xC8C8C8, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.event_id"),
                    leftX, panelY + 68, 0xC8C8C8, false);
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.condition_id"),
                    leftX, panelY + 104, 0xC8C8C8, false);
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.dimension"),
                    rightX, panelY + 68, 0xC8C8C8, false);
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.target"),
                    rightX, panelY + 104, 0xC8C8C8, false);
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.original_gate"),
                    leftX, panelY + 145, 0xFFB8B8FF, false);
            graphics.drawString(font, Component.translatable("gui.blacksouls.door.editor.original_unlock"),
                    leftX, panelY + 157, 0xFFB8B8FF, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component modeText() {
        return Component.translatable("gui.blacksouls.door.editor.mode." + mode.name().toLowerCase());
    }

    private Component consumeText() {
        return Component.translatable(consume
                ? "gui.blacksouls.door.editor.consume.on"
                : "gui.blacksouls.door.editor.consume.off");
    }

    private Component eventStateText() {
        return Component.translatable(eventTriggered
                ? "gui.blacksouls.door.editor.event_state.unlocked"
                : "gui.blacksouls.door.editor.event_state.locked");
    }

    private static String number(double value) {
        return Double.toString(value);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
