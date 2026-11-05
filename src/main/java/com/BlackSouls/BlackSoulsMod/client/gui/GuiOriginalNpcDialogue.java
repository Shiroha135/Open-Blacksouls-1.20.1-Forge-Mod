package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundOriginalNpcPacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundOriginalNpcTradePacket;
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
    private ShopPage shopPage = ShopPage.COMMAND;
    private ShopPage quantityReturn = ShopPage.BUY;
    private int sellCategory;
    private int quantity = 1;
    private int quantitySelection;
    private int quantityOffset;
    private ClientboundOriginalNpcPacket.TradeOffer quantityOffer;
    private List<ClientboundOriginalNpcPacket.TradeOffer> tradeOffers = List.of();
    private KeepPage keepPage = KeepPage.CATEGORY;
    private int keepCategory;
    private int keepDelta;
    private int keepSelection;
    private int keepOffset;
    private ClientboundOriginalNpcPacket.KeepOffer keepOffer;
    private List<ClientboundOriginalNpcPacket.KeepOffer> keepOffers = List.of();

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
        if (samePrompt && (!awaitingResponse || isItemKeep())) {
            return;
        }
        boolean previousKeep = isItemKeep();
        boolean nextKeep = next.type() == OriginalNpcInterpreter.Type.ITEM_KEEP && next.keepView() != null;
        boolean previousTrade = isTradeShop();
        boolean nextTrade = next.type() == OriginalNpcInterpreter.Type.SHOP && next.shopView() != null;
        boolean tradeReply = previousTrade && awaitingResponse;
        boolean changedStorage = previousTrade && nextTrade && packet.shopView().storage() != next.shopView().storage();
        boolean retainText = next.text().isEmpty()
                && (next.type() == OriginalNpcInterpreter.Type.CHOICE || next.type() == OriginalNpcInterpreter.Type.SHOP || nextKeep);
        if (!retainText) {
            currentText = next.text();
            currentPortrait = next.portrait();
            portraitIndex = next.portraitIndex();
            formattedText = formatOriginalText(currentText);
            portraitData = OriginalNpcTextures.portrait(currentPortrait);
            portraitTexture = OriginalNpcTextures.texture(portraitData);
            textPage = 0;
        }
        npcName = next.name();
        boolean sameShop = packet != null && packet.type() == OriginalNpcInterpreter.Type.SHOP
                && next.type() == OriginalNpcInterpreter.Type.SHOP
                && (packet.offers().equals(next.offers()) || previousTrade && nextTrade);
        if (packet == null || packet.type() != next.type() || !samePrompt && !sameShop && !(previousKeep && nextKeep)) {
            selected = 0;
            listOffset = 0;
        }
        packet = next;
        awaitingResponse = isSilentWait();
        if (nextKeep) {
            if (!previousKeep) {
                shopPage = ShopPage.COMMAND;
                quantityOffer = null;
                tradeOffers = List.of();
                keepPage = KeepPage.CATEGORY;
                keepCategory = 0;
                keepOffer = null;
                keepDelta = 0;
            } else if (keepPage == KeepPage.QUANTITY) {
                ClientboundOriginalNpcPacket.KeepOffer updated = packet.keepView().offers().stream()
                        .filter(offer -> offer.kind() == keepOffer.kind() && offer.id() == keepOffer.id())
                        .findFirst().orElse(null);
                if (updated == null || updated.storageForbidden()) {
                    closeKeepQuantity();
                } else {
                    keepOffer = updated;
                    keepDelta = Math.max(-updated.maxWithdraw(), Math.min(updated.maxDeposit(), keepDelta));
                }
            }
            rebuildKeepLabels();
            reflowText();
            return;
        }
        keepPage = KeepPage.CATEGORY;
        keepOffer = null;
        keepOffers = List.of();
        keepDelta = 0;
        if (nextTrade) {
            if (!previousTrade) {
                shopPage = ShopPage.COMMAND;
                sellCategory = 0;
                quantityOffer = null;
                quantity = 1;
            } else if (shopPage == ShopPage.QUANTITY) {
                ClientboundOriginalNpcPacket.TradeOffer updated = offersFor(quantityReturn).stream()
                        .filter(offer -> offer.kind() == quantityOffer.kind() && offer.id() == quantityOffer.id())
                        .findFirst().orElse(null);
                if (tradeReply || changedStorage || updated == null || !updated.enabled() || updated.maximum() == 0
                        || quantityReturn == ShopPage.SELL && packet.shopView().purchaseOnly()) {
                    closeQuantity();
                } else {
                    quantityOffer = updated;
                    quantity = Math.min(quantity, updated.maximum());
                }
            }
            if (packet.shopView().purchaseOnly() && (shopPage == ShopPage.SELL || shopPage == ShopPage.CATEGORY)) {
                shopPage = ShopPage.COMMAND;
                selected = 0;
                listOffset = 0;
            }
            if (changedStorage && shopPage == ShopPage.SELL) {
                selected = 0;
                listOffset = 0;
            }
            rebuildTradeLabels();
            reflowText();
            return;
        }
        shopPage = ShopPage.COMMAND;
        quantityOffer = null;
        tradeOffers = List.of();
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

    private boolean isItemKeep() {
        return packet != null && packet.type() == OriginalNpcInterpreter.Type.ITEM_KEEP && packet.keepView() != null;
    }

    private static Component keepText(String key, Object... values) {
        return Component.translatable("gui.blacksouls.item_keep." + key, values);
    }

    private void rebuildKeepLabels() {
        List<Component> next = new ArrayList<>();
        keepOffers = packet.keepView().offers().stream().filter(offer -> offer.category() == keepCategory).toList();
        if (keepPage == KeepPage.CATEGORY) {
            for (String category : List.of("items", "weapons", "armor", "key_items")) {
                next.add(keepText(category));
            }
        } else {
            keepOffers.forEach(offer -> next.add(formatOriginalText(offer.name())));
        }
        labels = List.copyOf(next);
        selected = Math.max(0, Math.min(selected, labels.size() - 1));
        listOffset = Math.max(0, Math.min(listOffset, Math.max(0, labels.size() - 1)));
    }

    private boolean keepRowEnabled(int index) {
        return keepPage == KeepPage.CATEGORY || index >= 0 && index < keepOffers.size()
                && !keepOffers.get(index).storageForbidden();
    }

    private void selectKeepRow() {
        if (awaitingResponse || !keepRowEnabled(selected)) return;
        if (keepPage == KeepPage.CATEGORY) {
            keepCategory = selected;
            keepPage = KeepPage.LIST;
            selected = 0;
            listOffset = 0;
        } else if (keepPage == KeepPage.LIST) {
            keepSelection = selected;
            keepOffset = listOffset;
            keepOffer = keepOffers.get(selected);
            keepDelta = 0;
            keepPage = KeepPage.QUANTITY;
        }
        rebuildKeepLabels();
    }

    private void closeKeepQuantity() {
        keepPage = KeepPage.LIST;
        selected = keepSelection;
        listOffset = keepOffset;
        keepOffer = null;
        keepDelta = 0;
        rebuildKeepLabels();
    }

    private void changeKeepQuantity(int amount) {
        if (keepOffer != null && !awaitingResponse) {
            keepDelta = Math.max(-keepOffer.maxWithdraw(), Math.min(keepOffer.maxDeposit(), keepDelta + amount));
        }
    }

    private void confirmKeepQuantity() {
        if (keepOffer == null || awaitingResponse) return;
        int delta = keepDelta;
        ClientboundOriginalNpcPacket.KeepOffer offer = keepOffer;
        closeKeepQuantity();
        if (delta != 0) {
            sendTrade(delta > 0 ? ServerboundOriginalNpcTradePacket.Action.DEPOSIT
                    : ServerboundOriginalNpcTradePacket.Action.WITHDRAW, offer.kind(), offer.id(), Math.abs(delta));
        }
    }

    private boolean isTradeShop() {
        return packet != null && packet.type() == OriginalNpcInterpreter.Type.SHOP && packet.shopView() != null;
    }

    private List<ClientboundOriginalNpcPacket.TradeOffer> offersFor(ShopPage page) {
        return page == ShopPage.BUY ? packet.shopView().buys()
                : packet.shopView().sells().stream().filter(offer -> offer.category() == sellCategory).toList();
    }

    private void rebuildTradeLabels() {
        List<Component> next = new ArrayList<>();
        tradeOffers = List.of();
        if (shopPage == ShopPage.COMMAND) {
            next.add(Component.literal("购入"));
            next.add(Component.literal("卖出"));
            next.add(Component.literal("返回"));
        } else if (shopPage == ShopPage.CATEGORY) {
            for (String category : List.of("道具", "武器", "防具", "重要物品", "返回")) {
                next.add(Component.literal(category));
            }
        } else if (shopPage == ShopPage.BUY || shopPage == ShopPage.SELL) {
            tradeOffers = offersFor(shopPage);
            tradeOffers.forEach(offer -> next.add(formatOriginalText(offer.name())));
            next.add(Component.literal("返回"));
        }
        labels = List.copyOf(next);
        selected = Math.max(0, Math.min(selected, labels.size() - 1));
        listOffset = Math.max(0, Math.min(listOffset, Math.max(0, labels.size() - 1)));
    }

    private boolean tradeRowEnabled(int index) {
        if (shopPage == ShopPage.COMMAND) return index != 1 || !packet.shopView().purchaseOnly();
        return index >= tradeOffers.size() || tradeOffers.get(index).enabled() && tradeOffers.get(index).maximum() > 0;
    }

    private void openShopPage(ShopPage page) {
        shopPage = page;
        selected = 0;
        listOffset = 0;
        rebuildTradeLabels();
    }

    private void selectTradeRow() {
        if (awaitingResponse || !tradeRowEnabled(selected)) return;
        if (shopPage == ShopPage.COMMAND) {
            if (selected == 0) openShopPage(ShopPage.BUY);
            else if (selected == 1) openShopPage(ShopPage.CATEGORY);
            else respond(-1);
        } else if (shopPage == ShopPage.CATEGORY) {
            if (selected == 4) openShopPage(ShopPage.COMMAND);
            else {
                sellCategory = selected;
                openShopPage(ShopPage.SELL);
            }
        } else if (shopPage == ShopPage.BUY || shopPage == ShopPage.SELL) {
            if (selected >= tradeOffers.size()) {
                cancelOrReturn();
            } else {
                quantityReturn = shopPage;
                quantitySelection = selected;
                quantityOffset = listOffset;
                quantityOffer = tradeOffers.get(selected);
                quantity = 1;
                shopPage = ShopPage.QUANTITY;
                rebuildTradeLabels();
            }
        }
    }

    private void closeQuantity() {
        shopPage = quantityReturn;
        selected = quantitySelection;
        listOffset = quantityOffset;
        quantityOffer = null;
        quantity = 1;
    }

    private void changeQuantity(int amount) {
        if (quantityOffer != null && !awaitingResponse) {
            quantity = Math.max(1, Math.min(quantityOffer.maximum(), quantity + amount));
        }
    }

    private void confirmQuantity() {
        if (quantityOffer == null || !quantityOffer.enabled() || quantityOffer.maximum() < 1 || awaitingResponse) return;
        sendTrade(quantityReturn == ShopPage.BUY ? ServerboundOriginalNpcTradePacket.Action.BUY
                : ServerboundOriginalNpcTradePacket.Action.SELL, quantityOffer.kind(), quantityOffer.id(), quantity);
    }

    private void sendTrade(ServerboundOriginalNpcTradePacket.Action action, int kind, int id, int amount) {
        if (closed || awaitingResponse) return;
        awaitingResponse = true;
        NetworkHandler.sendToServer(new ServerboundOriginalNpcTradePacket(entityId, sessionId, packet.revision(),
                action, kind, id, amount));
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        }
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
        boolean shop = packet.type() == OriginalNpcInterpreter.Type.SHOP || isItemKeep();
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
        if (!labels.isEmpty() || isItemKeep()) {
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
        if (isTradeShop() && shopPage == ShopPage.QUANTITY) renderQuantity(graphics, mouseX, mouseY);
        if (isItemKeep() && keepPage == KeepPage.QUANTITY) renderKeepQuantity(graphics, mouseX, mouseY);
        if (hoveredTooltip != null) {
            graphics.renderTooltip(font, hoveredTooltip, mouseX, mouseY);
        }
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        ListBox box = listBox();
        listOffset = Math.max(0, Math.min(listOffset, Math.max(0, labels.size() - box.visible())));
        BSGuiUtils.drawRMWindow(graphics, box.x(), box.y(), box.width(), box.height());
        long souls = isTradeShop() ? packet.shopView().money() : minecraft == null || minecraft.player == null ? 0L
                : minecraft.player.getCapability(BSPlayerStats.CAPABILITY).map(stats -> stats.souls).orElse(0L);
        if (box.header() > 0) {
            if (isItemKeep()) {
                graphics.drawString(font, keepText("title"), box.x() + PADDING, box.y() + PADDING, 0xFFFFFF, false);
                if (keepPage != KeepPage.CATEGORY) {
                    Component counters = keepText("columns");
                    graphics.drawString(font, counters, box.x() + box.width() - PADDING - font.width(counters),
                            box.y() + PADDING, 0xFFFFFF, false);
                }
            } else {
                String balance = "持有 " + souls + " S";
                graphics.drawString(font, balance, box.x() + PADDING, box.y() + PADDING, 0xFFFFFF, false);
                if (isTradeShop()) renderShopMode(graphics, box);
            }
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
            if (isItemKeep()) {
                if (!keepRowEnabled(index)) color = 0xAAAAAA;
                if (keepPage != KeepPage.CATEGORY && index < keepOffers.size()) {
                    var offer = keepOffers.get(index);
                    String right = offer.carried() + " / " + offer.stored() + " / " + offer.storageLimit();
                    labelWidth -= font.width(right) + 12;
                    graphics.drawString(font, right, box.x() + box.width() - PADDING - font.width(right), y + 4, color, false);
                }
            } else if (isTradeShop()) {
                if (!tradeRowEnabled(index)) color = 0xAAAAAA;
                if (index < tradeOffers.size()) {
                    var offer = tradeOffers.get(index);
                    String right = "×" + offer.owned() + "  " + offer.price() + " S";
                    labelWidth -= font.width(right) + 12;
                    graphics.drawString(font, right, box.x() + box.width() - PADDING - font.width(right), y + 4, color, false);
                }
            } else if (packet.type() == OriginalNpcInterpreter.Type.SHOP && index < packet.offers().size()) {
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

    private void renderShopMode(GuiGraphics graphics, ListBox box) {
        String mode = packet.shopView().storage() ? "寄存所" : "所持品";
        if (packet.shopView().canToggleStorage() && shopPage != ShopPage.QUANTITY) mode += " Z";
        graphics.drawString(font, mode, box.x() + box.width() - PADDING - font.width(mode),
                box.y() + PADDING, 0xFFFFFF, false);
    }

    private ListBox quantityBox() {
        int boxWidth = Math.min(Math.max(1, width - 4), Math.max(240, width * 3 / 5));
        int boxHeight = PADDING * 2 + ROW_HEIGHT * 5;
        return new ListBox(Math.max(2, width - boxWidth - 2), Math.max(2, (height - boxHeight) / 2),
                boxWidth, boxHeight, 4, ROW_HEIGHT);
    }

    private void renderQuantity(GuiGraphics graphics, int mouseX, int mouseY) {
        ListBox box = quantityBox();
        BSGuiUtils.drawRMWindow(graphics, box.x(), box.y(), box.width(), box.height());
        int x = box.x() + PADDING;
        int y = box.y() + PADDING;
        graphics.drawString(font, "持有 " + packet.shopView().money() + " S", x, y, 0xFFFFFF, false);
        renderShopMode(graphics, box);
        y += ROW_HEIGHT;
        Component label = formatOriginalText(quantityOffer.name());
        List<FormattedCharSequence> wrapped = font.split(label, Math.max(16, box.width() - PADDING * 2));
        if (!wrapped.isEmpty()) graphics.drawString(font, wrapped.get(0), x, y + 4, 0xFFFFFF, false);
        if (wrapped.size() > 1 && box.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW_HEIGHT) {
            hoveredTooltip = label;
        }
        y += ROW_HEIGHT;
        String number = "数量 " + quantity + " / " + quantityOffer.maximum();
        graphics.drawString(font, "−", x, y + 4, 0xFFFFFF, false);
        graphics.drawString(font, number, box.x() + (box.width() - font.width(number)) / 2, y + 4, 0xFFFFFF, false);
        graphics.drawString(font, "+", box.x() + box.width() - PADDING - font.width("+"), y + 4, 0xFFFFFF, false);
        y += ROW_HEIGHT;
        String total = "合计 " + java.math.BigInteger.valueOf(quantityOffer.price()).multiply(java.math.BigInteger.valueOf(quantity)) + " S";
        graphics.drawString(font, total, x, y + 4, 0xFFFFFF, false);
        y += ROW_HEIGHT;
        int middle = box.x() + box.width() / 2;
        if (!awaitingResponse && box.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW_HEIGHT) {
            graphics.fill(mouseX < middle ? x - 4 : middle, y + 1,
                    mouseX < middle ? middle : box.x() + box.width() - PADDING + 4, y + ROW_HEIGHT - 1, 0x55FFFFFF);
        }
        int color = awaitingResponse ? 0xAAAAAA : 0xFFFFFF;
        graphics.drawString(font, "确认", x, y + 4, color, false);
        graphics.drawString(font, "取消", middle + PADDING, y + 4, color, false);
    }

    private ListBox keepQuantityBox() {
        ListBox box = quantityBox();
        return new ListBox(box.x(), Math.max(2, (height - (PADDING * 2 + ROW_HEIGHT * 7)) / 2),
                box.width(), PADDING * 2 + ROW_HEIGHT * 7, 7, 0);
    }

    private void renderKeepQuantity(GuiGraphics graphics, int mouseX, int mouseY) {
        ListBox box = keepQuantityBox();
        BSGuiUtils.drawRMWindow(graphics, box.x(), box.y(), box.width(), box.height());
        int x = box.x() + PADDING;
        int y = box.y() + PADDING;
        int color = awaitingResponse ? 0xAAAAAA : 0xFFFFFF;
        Component label = formatOriginalText(keepOffer.name());
        List<FormattedCharSequence> wrapped = font.split(label, Math.max(16, box.width() - PADDING * 2));
        if (!wrapped.isEmpty()) graphics.drawString(font, wrapped.get(0), x, y + 4, color, false);
        if (wrapped.size() > 1 && box.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW_HEIGHT) {
            hoveredTooltip = label;
        }
        graphics.drawString(font, keepText("preview", keepOffer.carried() - keepDelta, keepOffer.stored() + keepDelta,
                keepOffer.storageLimit()), x, y + ROW_HEIGHT + 4, color, false);
        Component delta = keepText("delta", keepDelta > 0 ? "+" + keepDelta : Integer.toString(keepDelta));
        graphics.drawString(font, "−", x, y + ROW_HEIGHT * 2 + 4, color, false);
        graphics.drawString(font, delta, box.x() + (box.width() - font.width(delta)) / 2,
                y + ROW_HEIGHT * 2 + 4, color, false);
        graphics.drawString(font, "+", box.x() + box.width() - PADDING - font.width("+"),
                y + ROW_HEIGHT * 2 + 4, color, false);
        graphics.drawString(font, keepText("one"), x, y + ROW_HEIGHT * 3 + 4, color, false);
        graphics.drawString(font, keepText("ten"), x, y + ROW_HEIGHT * 4 + 4, color, false);
        graphics.drawString(font, keepText("hundred"), x, y + ROW_HEIGHT * 5 + 4, color, false);
        int middle = box.x() + box.width() / 2;
        int buttonsY = y + ROW_HEIGHT * 6;
        if (!awaitingResponse && box.contains(mouseX, mouseY) && mouseY >= buttonsY && mouseY < buttonsY + ROW_HEIGHT) {
            graphics.fill(mouseX < middle ? x - 4 : middle, buttonsY + 1,
                    mouseX < middle ? middle : box.x() + box.width() - PADDING + 4, buttonsY + ROW_HEIGHT - 1, 0x55FFFFFF);
        }
        graphics.drawString(font, keepText("confirm"), x, buttonsY + 4, color, false);
        graphics.drawString(font, keepText("cancel"), middle + PADDING, buttonsY + 4, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isSilentWait()) return true;
        if ((isTradeShop() || isItemKeep()) && awaitingResponse) return true;
        if (button == 1) {
            cancelOrReturn();
            return true;
        }
        if (isItemKeep() && keepPage == KeepPage.QUANTITY) {
            ListBox box = keepQuantityBox();
            int row = (int) ((mouseY - box.y() - PADDING) / ROW_HEIGHT);
            if (button == 0 && box.contains(mouseX, mouseY)) {
                if (row == 2) changeKeepQuantity(mouseX < box.x() + box.width() / 2.0D ? -1 : 1);
                else if (row == 6) {
                    if (mouseX < box.x() + box.width() / 2.0D) confirmKeepQuantity();
                    else cancelOrReturn();
                }
            }
            return true;
        }
        if (isTradeShop() && shopPage == ShopPage.QUANTITY) {
            ListBox box = quantityBox();
            int row = (int) ((mouseY - box.y() - PADDING - box.header()) / ROW_HEIGHT);
            if (button == 0 && box.contains(mouseX, mouseY)) {
                if (row == 1) changeQuantity(mouseX < box.x() + box.width() / 2.0D ? -1 : 1);
                else if (row == 3) {
                    if (mouseX < box.x() + box.width() / 2.0D) confirmQuantity();
                    else cancelOrReturn();
                }
            }
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
        if ((isTradeShop() || isItemKeep()) && awaitingResponse) return true;
        if (isItemKeep() && keepPage == KeepPage.QUANTITY) {
            if (delta != 0.0D) changeKeepQuantity(delta > 0.0D ? 1 : -1);
            return true;
        }
        if (isTradeShop() && shopPage == ShopPage.QUANTITY) {
            if (delta != 0.0D) changeQuantity(delta > 0.0D ? 1 : -1);
            return true;
        }
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
        if (isItemKeep()) {
            if (awaitingResponse) return true;
            if (keyCode == GLFW.GLFW_KEY_Z) return true;
            if (keepPage == KeepPage.CATEGORY && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
                selected = Math.floorMod(selected + (keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1), 4);
                keepSelectionVisible();
                return true;
            }
            if (keepPage == KeepPage.QUANTITY) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) cancelOrReturn();
                else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                    confirmKeepQuantity();
                } else if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                        || (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) changeKeepQuantity(100);
                else if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
                        || (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) changeKeepQuantity(-100);
                else if (keyCode == GLFW.GLFW_KEY_UP) changeKeepQuantity(1);
                else if (keyCode == GLFW.GLFW_KEY_DOWN) changeKeepQuantity(-1);
                else if (keyCode == GLFW.GLFW_KEY_RIGHT) changeKeepQuantity(10);
                else if (keyCode == GLFW.GLFW_KEY_LEFT) changeKeepQuantity(-10);
                return true;
            }
        }
        if (isTradeShop()) {
            if (awaitingResponse) return true;
            if (keyCode == GLFW.GLFW_KEY_Z) {
                if (shopPage != ShopPage.QUANTITY && packet.shopView().canToggleStorage()) {
                    sendTrade(ServerboundOriginalNpcTradePacket.Action.TOGGLE_STORAGE, 0, 0, 0);
                }
                return true;
            }
            if (shopPage == ShopPage.QUANTITY) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) cancelOrReturn();
                else if (keyCode == GLFW.GLFW_KEY_LEFT) changeQuantity(-1);
                else if (keyCode == GLFW.GLFW_KEY_RIGHT) changeQuantity(1);
                else if (keyCode == GLFW.GLFW_KEY_UP) changeQuantity(10);
                else if (keyCode == GLFW.GLFW_KEY_DOWN) changeQuantity(-10);
                else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                    confirmQuantity();
                }
                return true;
            }
        }
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
        if (isItemKeep()) {
            selectKeepRow();
        } else if (isTradeShop()) {
            selectTradeRow();
        } else if (packet.type() == OriginalNpcInterpreter.Type.CHOICE && selected < packet.choices().size()) {
            respond(selected);
        } else if (packet.type() == OriginalNpcInterpreter.Type.SHOP) {
            respond(selected < packet.offers().size() ? selected : -1);
        }
    }

    private void cancelOrReturn() {
        if (isItemKeep()) {
            if (awaitingResponse) return;
            if (keepPage == KeepPage.QUANTITY) {
                closeKeepQuantity();
            } else if (keepPage == KeepPage.LIST) {
                keepPage = KeepPage.CATEGORY;
                selected = keepCategory;
                listOffset = 0;
                rebuildKeepLabels();
            } else {
                respond(-1);
            }
        } else if (isTradeShop()) {
            if (awaitingResponse) return;
            if (shopPage == ShopPage.QUANTITY) {
                closeQuantity();
                rebuildTradeLabels();
            } else if (shopPage == ShopPage.BUY || shopPage == ShopPage.CATEGORY) {
                openShopPage(ShopPage.COMMAND);
            } else if (shopPage == ShopPage.SELL) {
                openShopPage(ShopPage.CATEGORY);
            } else {
                respond(-1);
            }
        } else if (packet.type() == OriginalNpcInterpreter.Type.CHOICE) {
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
        if (isTradeShop() || isItemKeep()) {
            cancelOrReturn();
            return;
        }
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

    private enum KeepPage {
        CATEGORY,
        LIST,
        QUANTITY
    }

    private enum ShopPage {
        COMMAND,
        BUY,
        CATEGORY,
        SELL,
        QUANTITY
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
