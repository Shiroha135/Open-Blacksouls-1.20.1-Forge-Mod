package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundOriginalNpcPacket;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcInterpreter;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcTextures;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GuiOriginalNpcDialogue extends Screen {
    private static final int PADDING = 12;
    private static final int LINE_HEIGHT = 14;
    private static final int ROW_HEIGHT = 18;
    private static final int[] TEXT_COLORS = {
            0xFFFFFF, 0x20A0D6, 0xFF784C, 0x66CC40, 0x99CCFF, 0xCCC0FF, 0xFFFFA0, 0x808080,
            0xC0C0C0, 0x2080CC, 0xFF3810, 0x00A010, 0x3E9ADE, 0xA098FF, 0xFFCC20, 0x000000,
            0x84AAFF, 0xFFFF40, 0xFF2020, 0x202040, 0xE08040, 0xF0C040, 0x4080C0, 0x40C0F0,
            0x80FF80, 0xC08080, 0x8080FF, 0xFF80FF, 0x00A040, 0x00E060, 0xA060E0, 0xC080FF
    };
    private final int entityId;
    private final UUID sessionId;
    private ClientboundOriginalNpcPacket packet;
    private String currentText = "";
    private String currentPortrait = "";
    private int portraitIndex;
    private String npcName = "";
    private Component formattedText = Component.empty();
    private List<FormattedCharSequence> lines = List.of();
    private List<Component> labels = List.of();
    private JsonObject portraitData;
    private ResourceLocation portraitTexture;
    private Component hoveredTooltip;
    private int textPage;
    private int selected;
    private int listOffset;
    private boolean awaitingResponse;
    private boolean closed;

    public GuiOriginalNpcDialogue(ClientboundOriginalNpcPacket packet) {
        super(Component.literal(packet.name()));
        this.entityId = packet.entityId();
        this.sessionId = packet.sessionId();
        update(packet);
    }

    public boolean isConversation(int entityId, UUID token) {
        return this.entityId == entityId && Objects.equals(sessionId, token);
    }

    public boolean isSilentWait() {
        return packet != null && (packet.type() == OriginalNpcInterpreter.Type.WAIT
                || packet.type() == OriginalNpcInterpreter.Type.ROUTE_WAIT
                || packet.type() == OriginalNpcInterpreter.Type.ANIMATION_WAIT);
    }

    public void update(ClientboundOriginalNpcPacket next) {
        if (!isConversation(next.entityId(), next.sessionId())
                || packet != null && next.revision() < packet.revision()) {
            return;
        }
        boolean samePrompt = packet != null && next.revision() == packet.revision();
        if (samePrompt && !awaitingResponse) {
            return;
        }
        boolean retainText = next.text().isEmpty()
                && (next.type() == OriginalNpcInterpreter.Type.CHOICE || next.type() == OriginalNpcInterpreter.Type.SHOP);
        if (!retainText) {
            currentText = next.text();
            currentPortrait = next.portrait();
            portraitIndex = next.portraitIndex();
            formattedText = formatOriginalText(currentText);
            portraitData = OriginalNpcTextures.portrait(currentPortrait);
            portraitTexture = OriginalNpcTextures.texture(portraitData);
            textPage = 0;
        }
        String displayName = OriginalNpcTextures.displayName(currentPortrait);
        npcName = displayName.isEmpty() ? next.name() : displayName;
        boolean sameShop = packet != null && packet.type() == OriginalNpcInterpreter.Type.SHOP
                && next.type() == OriginalNpcInterpreter.Type.SHOP && packet.offers().equals(next.offers());
        if (packet == null || packet.type() != next.type() || !samePrompt && !sameShop) {
            selected = 0;
            listOffset = 0;
        }
        packet = next;
        awaitingResponse = isSilentWait();
        List<Component> nextLabels = new ArrayList<>();
        if (next.type() == OriginalNpcInterpreter.Type.CHOICE) {
            next.choices().forEach(value -> nextLabels.add(formatOriginalText(value)));
        } else if (next.type() == OriginalNpcInterpreter.Type.SHOP) {
            next.offers().forEach(offer -> nextLabels.add(formatOriginalText(offer.name())));
            nextLabels.add(Component.literal("返回"));
        }
        labels = List.copyOf(nextLabels);
        selected = Math.max(0, Math.min(selected, labels.size() - 1));
        reflowText();
    }

    @Override
    protected void init() {
        super.init();
        reflowText();
    }

    private int mainHeight() {
        return Math.min(90, Math.max(48, height / 2));
    }

    private int mainTop() {
        return height - mainHeight();
    }

    private int portraitSize() {
        return Math.min(70, Math.max(20, mainHeight() - PADDING * 2));
    }

    private int textLeft() {
        return PADDING + (portraitTexture == null ? 0 : portraitSize() + 13);
    }

    private int linesPerPage() {
        return Math.max(1, (mainHeight() - PADDING * 2) / LINE_HEIGHT);
    }

    private int pageCount() {
        return Math.max(1, (lines.size() + linesPerPage() - 1) / linesPerPage());
    }

    private void reflowText() {
        if (font != null) {
            lines = font.split(formattedText, Math.max(16, width - textLeft() - PADDING));
            textPage = Math.max(0, Math.min(textPage, pageCount() - 1));
        }
    }

    private ListBox listBox() {
        boolean shop = packet.type() == OriginalNpcInterpreter.Type.SHOP;
        int desiredWidth = shop ? Math.max(240, width * 3 / 5) : 140;
        if (!shop) {
            for (Component label : labels) {
                desiredWidth = Math.max(desiredWidth, font.width(label) + PADDING * 2);
            }
        }
        int boxWidth = Math.min(width, desiredWidth);
        int header = shop ? ROW_HEIGHT : 0;
        int available = Math.max(ROW_HEIGHT + PADDING * 2 + header, mainTop() - 24);
        int visible = Math.min(Math.max(1, labels.size()),
                Math.max(1, (available - PADDING * 2 - header) / ROW_HEIGHT));
        int boxHeight = PADDING * 2 + header + visible * ROW_HEIGHT;
        return new ListBox(width - boxWidth, Math.max(2, mainTop() - 16 - boxHeight),
                boxWidth, boxHeight, visible, header);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isSilentWait()) return;
        hoveredTooltip = null;
        BSGuiUtils.drawRMWindow(graphics, 0, mainTop(), width, mainHeight());
        if (!labels.isEmpty()) {
            renderList(graphics, mouseX, mouseY);
        }
        if (!npcName.isEmpty()) {
            String name = font.plainSubstrByWidth(npcName, Math.max(1, width - 54));
            int nameWidth = font.width(name) + 24;
            BSGuiUtils.drawRMWindow(graphics, 15, mainTop() - 10, nameWidth, 22);
            graphics.drawString(font, name, 27, mainTop() - 3, 0xFFFFFF, false);
        }
        if (portraitTexture != null && portraitData != null) {
            int index = Math.max(0, Math.min(7, portraitIndex));
            RenderSystem.enableBlend();
            graphics.blit(portraitTexture, PADDING, mainTop() + PADDING, portraitSize(), portraitSize(),
                    (index % 4) * 96, (index / 4) * 96, 96, 96,
                    portraitData.get("width").getAsInt(), portraitData.get("height").getAsInt());
            RenderSystem.disableBlend();
        }
        graphics.enableScissor(textLeft(), mainTop() + PADDING, width - PADDING, height - 10);
        int first = textPage * linesPerPage();
        int last = Math.min(lines.size(), first + linesPerPage());
        for (int index = first; index < last; index++) {
            graphics.drawString(font, lines.get(index), textLeft(), mainTop() + PADDING
                    + (index - first) * LINE_HEIGHT, 0xFFFFFF, false);
        }
        graphics.disableScissor();
        if (pageCount() > 1) {
            String page = (textPage + 1) + " / " + pageCount();
            graphics.drawString(font, page, width - PADDING - font.width(page), height - 11, 0xAAAAAA, false);
        } else if (packet.type() == OriginalNpcInterpreter.Type.TEXT && !awaitingResponse
                && (net.minecraft.Util.getMillis() / 300L) % 2L == 0L) {
            graphics.drawString(font, "▼", width - PADDING - 9, height - 13, 0xAAAAAA, false);
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (hoveredTooltip != null) {
            graphics.renderTooltip(font, hoveredTooltip, mouseX, mouseY);
        }
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        ListBox box = listBox();
        listOffset = Math.max(0, Math.min(listOffset, Math.max(0, labels.size() - box.visible())));
        BSGuiUtils.drawRMWindow(graphics, box.x(), box.y(), box.width(), box.height());
        long souls = minecraft == null || minecraft.player == null ? 0L
                : minecraft.player.getCapability(BSPlayerStats.CAPABILITY).map(stats -> stats.souls).orElse(0L);
        if (box.header() > 0) {
            String balance = "持有 " + souls + " S";
            graphics.drawString(font, balance, box.x() + PADDING, box.y() + PADDING, 0xFFFFFF, false);
        }
        for (int row = 0; row < box.visible() && row + listOffset < labels.size(); row++) {
            int index = row + listOffset;
            int y = box.y() + PADDING + box.header() + row * ROW_HEIGHT;
            boolean hovered = box.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (!awaitingResponse && (hovered || index == selected)) {
                graphics.fill(box.x() + 5, y + 1, box.x() + box.width() - 5, y + ROW_HEIGHT - 1, 0x55FFFFFF);
            }
            int labelWidth = box.width() - PADDING * 2;
            int color = awaitingResponse ? 0xAAAAAA : 0xFFFFFF;
            if (packet.type() == OriginalNpcInterpreter.Type.SHOP && index < packet.offers().size()) {
                long price = packet.offers().get(index).price();
                String priceText = price + " S";
                labelWidth -= font.width(priceText) + 12;
                if (price > souls) {
                    color = 0xAAAAAA;
                }
                graphics.drawString(font, priceText, box.x() + box.width() - PADDING - font.width(priceText),
                        y + 4, color, false);
            }
            List<FormattedCharSequence> labelLines = font.split(labels.get(index), Math.max(16, labelWidth));
            if (!labelLines.isEmpty()) {
                graphics.drawString(font, labelLines.get(0), box.x() + PADDING, y + 4, color, false);
            }
            if (hovered && labelLines.size() > 1) {
                hoveredTooltip = labels.get(index);
            }
        }
        if (listOffset > 0) {
            graphics.drawString(font, "↑", box.x() + box.width() - 11, box.y() + 2, 0xAAAAAA, false);
        }
        if (listOffset + box.visible() < labels.size()) {
            graphics.drawString(font, "↓", box.x() + box.width() - 11, box.y() + box.height() - 11, 0xAAAAAA, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isSilentWait()) return true;
        if (button == 1) {
            cancelOrReturn();
            return true;
        }
        if (button != 0 || awaitingResponse) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (!labels.isEmpty()) {
            ListBox box = listBox();
            int row = (int) ((mouseY - box.y() - PADDING - box.header()) / ROW_HEIGHT);
            if (box.contains(mouseX, mouseY) && mouseY >= box.y() + PADDING + box.header()
                    && row >= 0 && row < box.visible() && row + listOffset < labels.size()) {
                selected = row + listOffset;
                chooseSelected();
                return true;
            }
        }
        if (mouseY >= mainTop() && mouseX >= 0 && mouseX < width) {
            advanceText();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isSilentWait()) return true;
        if (delta == 0.0D) {
            return false;
        }
        int direction = delta > 0.0D ? -1 : 1;
        if (!labels.isEmpty() && listBox().contains(mouseX, mouseY)) {
            ListBox box = listBox();
            listOffset = Math.max(0, Math.min(listOffset + direction, Math.max(0, labels.size() - box.visible())));
            selected = Math.max(listOffset, Math.min(selected, listOffset + box.visible() - 1));
        } else {
            textPage = Math.max(0, Math.min(textPage + direction, pageCount() - 1));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isSilentWait()) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelOrReturn();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            textPage = Math.max(0, textPage - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            textPage = Math.min(pageCount() - 1, textPage + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (!labels.isEmpty()) {
                chooseSelected();
            } else {
                advanceText();
            }
            return true;
        }
        int step = keyCode == GLFW.GLFW_KEY_UP ? -1 : keyCode == GLFW.GLFW_KEY_DOWN ? 1 : 0;
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            step = keyCode == GLFW.GLFW_KEY_PAGE_UP ? -1 : 1;
            step *= labels.isEmpty() ? 1 : listBox().visible();
        }
        if (step != 0) {
            if (labels.isEmpty()) {
                textPage = Math.max(0, Math.min(textPage + step, pageCount() - 1));
            } else {
                selected = Math.floorMod(selected + step, labels.size());
                keepSelectionVisible();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void keepSelectionVisible() {
        int visible = listBox().visible();
        if (selected < listOffset) {
            listOffset = selected;
        } else if (selected >= listOffset + visible) {
            listOffset = selected - visible + 1;
        }
    }

    private void advanceText() {
        if (textPage + 1 < pageCount()) {
            textPage++;
        } else if (packet.type() == OriginalNpcInterpreter.Type.TEXT) {
            respond(-1);
        }
    }

    private void chooseSelected() {
        if (packet.type() == OriginalNpcInterpreter.Type.CHOICE && selected < packet.choices().size()) {
            respond(selected);
        } else if (packet.type() == OriginalNpcInterpreter.Type.SHOP) {
            respond(selected < packet.offers().size() ? selected : -1);
        }
    }

    private void cancelOrReturn() {
        if (packet.type() == OriginalNpcInterpreter.Type.CHOICE) {
            respond(-2);
        } else if (packet.type() == OriginalNpcInterpreter.Type.SHOP) {
            respond(-1);
        } else {
            onClose();
        }
    }

    private void respond(int selection) {
        if (closed || awaitingResponse) {
            return;
        }
        awaitingResponse = true;
        NetworkHandler.sendToServer(new ServerboundOriginalNpcPacket(entityId, sessionId, packet.revision(), selection));
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        }
    }

    @Override
    public void onClose() {
        if (isSilentWait()) return;
        if (!closed) {
            closed = true;
            NetworkHandler.sendToServer(new ServerboundOriginalNpcPacket(entityId, sessionId, packet.revision(), -3));
        }
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component formatOriginalText(String raw) {
        MutableComponent result = Component.empty();
        StringBuilder part = new StringBuilder();
        Style style = Style.EMPTY.withColor(TEXT_COLORS[0]);
        for (int index = 0; index < raw.length();) {
            char character = raw.charAt(index);
            if (character == '\u00A7' && index + 1 < raw.length()) {
                ChatFormatting format = ChatFormatting.getByCode(raw.charAt(index + 1));
                if (format != null) {
                    result.append(Component.literal(part.toString()).setStyle(style));
                    part.setLength(0);
                    style = style.applyFormat(format);
                    index += 2;
                    continue;
                }
            }
            if (character == '\\' && index + 1 < raw.length()) {
                char code = raw.charAt(index + 1);
                if ((code == 'c' || code == 'C') && index + 2 < raw.length() && raw.charAt(index + 2) == '[') {
                    int end = raw.indexOf(']', index + 3);
                    if (end > index + 3) {
                        try {
                            int color = Integer.parseInt(raw.substring(index + 3, end));
                            result.append(Component.literal(part.toString()).setStyle(style));
                            part.setLength(0);
                            style = Style.EMPTY.withColor(TEXT_COLORS[Math.max(0, Math.min(TEXT_COLORS.length - 1, color))]);
                            index = end + 1;
                            continue;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                if (".|!><^{}".indexOf(code) >= 0) {
                    index += 2;
                    continue;
                }
                if (code == '\\') {
                    part.append('\\');
                    index += 2;
                    continue;
                }
            }
            if (character != '\r') {
                part.append(character);
            }
            index++;
        }
        return result.append(Component.literal(part.toString()).setStyle(style));
    }

    private record ListBox(int x, int y, int width, int height, int visible, int header) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
