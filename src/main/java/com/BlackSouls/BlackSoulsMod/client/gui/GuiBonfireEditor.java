package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.capability.BonfireEntry;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketUpdateBonfireName;
import com.BlackSouls.BlackSoulsMod.util.BonfireMetadata;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class GuiBonfireEditor extends Screen {
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 196;
    private static final int PADDING = 18;
    private final BonfireEntry entry;
    private EditBox nameField;
    private MultiLineEditBox descriptionField;
    private int guiLeft;
    private int guiTop;

    public GuiBonfireEditor(BonfireEntry entry) {
        super(Component.translatable("gui.blacksouls.bonfire.editor.title"));
        this.entry = entry;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        int fieldX = guiLeft + PADDING;
        int fieldWidth = GUI_WIDTH - PADDING * 2;

        this.nameField = new EditBox(
                this.font,
                fieldX,
                guiTop + 42,
                fieldWidth,
                18,
                Component.translatable("gui.blacksouls.bonfire.editor.name")
        );
        this.nameField.setMaxLength(50);
        this.nameField.setValue(displayName(entry.name));
        this.addRenderableWidget(nameField);

        this.descriptionField = new MultiLineEditBox(
                this.font,
                fieldX,
                guiTop + 82,
                fieldWidth,
                66,
                Component.empty(),
                Component.translatable("gui.blacksouls.bonfire.desc_hint")
        );
        this.descriptionField.setCharacterLimit(1024);
        this.descriptionField.setValue(entry.description == null ? "" : entry.description);
        this.addRenderableWidget(descriptionField);

        int buttonY = guiTop + GUI_HEIGHT - 30;
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> saveAndClose()
        ).bounds(guiLeft + GUI_WIDTH / 2 - 92, buttonY, 88, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
        ).bounds(guiLeft + GUI_WIDTH / 2 + 4, buttonY, 88, 20).build());
        this.setInitialFocus(nameField);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        BSGuiUtils.drawRMWindow(graphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, guiTop + 15, 0xFFFFFF);
        graphics.drawString(
                this.font,
                Component.translatable("gui.blacksouls.bonfire.editor.name"),
                guiLeft + PADDING,
                guiTop + 31,
                0xC8C8C8,
                false
        );
        graphics.drawString(
                this.font,
                Component.translatable("gui.blacksouls.bonfire.editor.description"),
                guiLeft + PADDING,
                guiTop + 71,
                0xC8C8C8,
                false
        );
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void saveAndClose() {
        String name = nameField.getValue().strip();
        if (name.isBlank() || name.equals(I18n.get(BonfireMetadata.DEFAULT_NAME))) {
            name = BonfireMetadata.DEFAULT_NAME;
        }
        NetworkHandler.INSTANCE.sendToServer(new PacketUpdateBonfireName(
                entry.pos,
                name,
                descriptionField.getValue().strip()
        ));
        this.onClose();
    }

    private static String displayName(String name) {
        if (name == null || name.isBlank()) {
            return I18n.get(BonfireMetadata.DEFAULT_NAME);
        }
        return name.startsWith("gui.blacksouls.") ? I18n.get(name) : name;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
