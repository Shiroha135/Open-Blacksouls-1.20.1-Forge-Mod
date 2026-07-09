package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.BSItemSellRegistry;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundTradePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

public class GuiUniversalShop extends Screen {

    public enum ShopType {
        COLORED_SOULS("gui.blacksouls.shop.title.souls"),
        CLOCK_MAKER("gui.blacksouls.shop.title.clock");
        final String titleKey;
        ShopType(String titleKey) { this.titleKey = titleKey; }
    }

    private final ShopType shopType;

    private int descH, tabH, subTabH, listW, baseListH;
    private final int soulWindowWidth = 150;
    private int soulWindowHeight;

    private static final int COLOR_TEXT_HIGHLIGHT = 0xFFFFFF;
    private static final int COLOR_TEXT_NORMAL = 0xAAAAAA;
    private static final int COLOR_TITLE_BLUE = 0x55AAFF;
    private static final int COLOR_S_SUFFIX = 0x5555FF;

    private static class ShopItem {
        final RegistryObject<Item> itemReg;
        final long price;
        final String descKey;
        ShopItem(RegistryObject<Item> itemReg, long price, String descKey) {
            this.itemReg = itemReg;
            this.price = price;
            this.descKey = descKey;
        }
    }

    private final List<ShopItem> shopItems = new ArrayList<>();
    private final List<ItemStack> playerSellableItems = new ArrayList<>();

    private int currentTab = 0;
    private int currentSubTab = 0;
    private int selectedIndex = 0;
    private long currentSouls = 0;

    private boolean isSelectingQuantity = false;
    private int actionQuantity = 1;

    private final List<String> cachedDescKeys = new ArrayList<>();

    public GuiUniversalShop(ShopType shopType) {
        super(Component.translatable(shopType.titleKey));
        this.shopType = shopType;
    }

    @Override
    protected void init() {
        super.init();
        this.descH = (int) (this.height * 0.15f);
        this.tabH = (int) (this.height * 0.12f);
        this.subTabH = (int) (this.height * 0.08f);
        this.listW = (int) (this.width * 0.4f);
        this.baseListH = this.height - descH - tabH;
        this.soulWindowHeight = this.tabH;

        this.isSelectingQuantity = false;
        this.actionQuantity = 1;

        if (this.minecraft.player != null) {
            this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> this.currentSouls = stats.souls);
        }

        initShopItems();
        updatePlayerInventoryList();
    }

    private void initShopItems() {
        shopItems.clear();
        if (this.shopType == ShopType.COLORED_SOULS) {
            shopItems.add(new ShopItem(BlackSouls.SOUL_GREEN, 5000, "lore.blacksouls.soul_green"));
            shopItems.add(new ShopItem(BlackSouls.SOUL_PURPLE, 5000, "lore.blacksouls.soul_purple"));
            shopItems.add(new ShopItem(BlackSouls.SOUL_RED, 5000, "lore.blacksouls.soul_red"));
            shopItems.add(new ShopItem(BlackSouls.SOUL_BLUE, 5000, "lore.blacksouls.soul_blue"));
            shopItems.add(new ShopItem(BlackSouls.SOUL_YELLOW, 5000, "lore.blacksouls.soul_yellow"));
            shopItems.add(new ShopItem(BlackSouls.SOUL_GRAY, 5000, "lore.blacksouls.soul_gray"));
            shopItems.add(new ShopItem(BlackSouls.SOUL_WHITE, 5000, "lore.blacksouls.soul_white"));
            shopItems.add(new ShopItem(BlackSouls.SOUL_FOUR_LEAF_CLOVER, 5000, "lore.blacksouls.soul_four_leaf_clover"));
        } else if (this.shopType == ShopType.CLOCK_MAKER) {
            shopItems.add(new ShopItem(BlackSouls.RABBIT_WATCH, 5000, "lore.blacksouls.rabbit_watch"));
        }
    }

    private void updatePlayerInventoryList() {
        playerSellableItems.clear();
        if (this.minecraft.player == null) return;

        for (ItemStack stack : this.minecraft.player.getInventory().items) {
            if (!stack.isEmpty() && BSItemSellRegistry.SELL_PRICES.containsKey(stack.getItem())) {
                if (getItemSubTabType(stack) == currentSubTab) {
                    playerSellableItems.add(stack);
                }
            }
        }
        clampSelectedIndex();
        updateSelectionState();
    }

    private void clampSelectedIndex() {
        if (currentTab == 1 && selectedIndex >= playerSellableItems.size()) {
            selectedIndex = Math.max(0, playerSellableItems.size() - 1);
            isSelectingQuantity = false;
        }
        if (currentTab == 0 && selectedIndex >= shopItems.size()) {
            selectedIndex = Math.max(0, shopItems.size() - 1);
            isSelectingQuantity = false;
        }
    }

    private void updateSelectionState() {
        cachedDescKeys.clear();
        if (this.minecraft.player == null) return;

        if (currentTab == 0 && !shopItems.isEmpty()) {
            ShopItem item = shopItems.get(selectedIndex);
            if (item.descKey != null && !item.descKey.isEmpty()) {
                cachedDescKeys.add(item.descKey);
            }
        } else if (currentTab == 1 && !playerSellableItems.isEmpty()) {
            ItemStack stack = playerSellableItems.get(selectedIndex);
            Item sellItem = stack.getItem();

            BSItemSellRegistry.SellInfo info = BSItemSellRegistry.SELL_PRICES.get(sellItem);
            if (info != null && info.descKey != null && !info.descKey.isEmpty()) {
                cachedDescKeys.add(info.descKey);
            } else {
                cachedDescKeys.addAll(BSItemSellRegistry.getDefaultLoreKeys(sellItem));
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        if (this.minecraft.player != null) {
            this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> this.currentSouls = stats.souls);
        }

        this.renderBackground(guiGraphics);

        int currentListY = descH + tabH;
        int currentListH = baseListH;

        renderBaseWindows(guiGraphics, currentListY, currentListH);
        renderTabs(guiGraphics);
        renderSoulWindow(guiGraphics);

        if (currentTab == 1) {
            renderSubTabs(guiGraphics, currentListY);
            currentListY += subTabH;
            currentListH -= subTabH;
        }

        renderLoreText(guiGraphics);

        long currentOwnedCount = 0;
        if (this.minecraft.player != null) {
            if (currentTab == 0 && !shopItems.isEmpty()) {
                currentOwnedCount = getInventoryCountFor(this.minecraft.player, new ItemStack(shopItems.get(selectedIndex).itemReg.get()));
            } else if (currentTab == 1 && !playerSellableItems.isEmpty()) {
                ItemStack stack = playerSellableItems.get(selectedIndex);
                currentOwnedCount = getInventoryCountFor(this.minecraft.player, stack);
                if (currentOwnedCount == 0 && !isSelectingQuantity) {
                    updatePlayerInventoryList();
                    updateSelectionState();
                }
            }
        }

        if (currentTab == 0 || (currentTab == 1 && isSelectingQuantity)) {
            if ((currentTab == 0 && !shopItems.isEmpty()) || (currentTab == 1 && !playerSellableItems.isEmpty())) {
                drawItemDetails(guiGraphics, currentOwnedCount, currentListY);
            }
        }

        int listStartX = 8;
        int listStartY = currentListY + 8;
        if (currentTab == 0) {
            drawBuyItemList(guiGraphics, listStartX, listStartY);
        } else if (currentTab == 1) {
            drawSellPlayerInventoryList(guiGraphics, listStartX, listStartY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderBaseWindows(GuiGraphics guiGraphics, int listY, int listH) {
        BSGuiUtils.drawRMWindow(guiGraphics, 0, 0, this.width, descH);
        BSGuiUtils.drawRMWindow(guiGraphics, 0, descH, this.width, tabH);

        if (currentTab == 0 || (currentTab == 1 && isSelectingQuantity)) {
            BSGuiUtils.drawRMWindow(guiGraphics, 0, listY, listW, listH);
            BSGuiUtils.drawRMWindow(guiGraphics, listW, listY, this.width - listW, listH);
        } else {
            BSGuiUtils.drawRMWindow(guiGraphics, 0, listY, this.width, listH);
        }
    }

    private void renderTabs(GuiGraphics guiGraphics) {
        int tabCenterY = descH + (tabH / 2) - 4;
        int buyTabX = (int) (this.width * 0.15f);
        int sellTabX = (int) (this.width * 0.35f);
        int cancelTabX = (int) (this.width * 0.55f);

        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.shop.tab.buy"), buyTabX, tabCenterY, currentTab == 0 ? COLOR_TEXT_HIGHLIGHT : COLOR_TEXT_NORMAL, false);
        guiGraphics.drawString(font, Component.translatable("gui.blacksouls.shop.tab.sell"), sellTabX, tabCenterY, currentTab == 1 ? COLOR_TEXT_HIGHLIGHT : COLOR_TEXT_NORMAL, false);

        Component cancelTabComp = Component.translatable("gui.blacksouls.shop.tab.cancel");
        int cancelBtnW = font.width(cancelTabComp) + 16;
        guiGraphics.fill(cancelTabX - 8, tabCenterY - 6, cancelTabX + cancelBtnW - 8, tabCenterY + 10, 0xAA222222);
        guiGraphics.drawString(font, cancelTabComp, cancelTabX, tabCenterY, COLOR_TEXT_NORMAL, false);
    }

    private void renderSubTabs(GuiGraphics guiGraphics, int listY) {
        BSGuiUtils.drawRMWindow(guiGraphics, 0, listY, this.width, subTabH);
        String[] subTabTexts = {
                I18n.get("gui.blacksouls.shop.sell.sub_tab.prop"),
                I18n.get("gui.blacksouls.shop.sell.sub_tab.weapon"),
                I18n.get("gui.blacksouls.shop.sell.sub_tab.armor"),
                I18n.get("gui.blacksouls.shop.sell.sub_tab.essential")
        };
        int subTabCenterY = listY + (subTabH / 2) - 4;
        int sectionWidth = this.width / 4;
        for (int i = 0; i < 4; i++) {
            int textColor = (i == currentSubTab) ? COLOR_TEXT_HIGHLIGHT : COLOR_TEXT_NORMAL;
            int textX = (i * sectionWidth) + (sectionWidth / 2) - (font.width(subTabTexts[i]) / 2);
            guiGraphics.drawString(font, subTabTexts[i], textX, subTabCenterY, textColor, false);
        }
    }

    private void renderSoulWindow(GuiGraphics guiGraphics) {
        int soulWindowX = this.width - soulWindowWidth;
        BSGuiUtils.drawRMWindow(guiGraphics, soulWindowX, descH, soulWindowWidth, soulWindowHeight);

        Component soulTxtComp = Component.literal(String.valueOf(currentSouls));
        int totalWidth = font.width(soulTxtComp) + font.width(" S");
        int startX = soulWindowX + soulWindowWidth - 15 - totalWidth;
        int soulTextY = descH + (soulWindowHeight / 2) - 4;

        guiGraphics.drawString(font, soulTxtComp, startX, soulTextY, COLOR_TEXT_HIGHLIGHT, false);
        guiGraphics.drawString(font, " S", startX + font.width(soulTxtComp), soulTextY, COLOR_S_SUFFIX, false);
    }

    private void renderLoreText(GuiGraphics guiGraphics) {
        if (!cachedDescKeys.isEmpty()) {
            int textDrawY = 12;
            boolean renderedAnyLine = false;

            for (String descKey : cachedDescKeys) {
                if (!I18n.exists(descKey)) {
                    continue;
                }

                String desc = I18n.get(descKey);
                List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(desc), this.width - 24);
                for (net.minecraft.util.FormattedCharSequence line : lines) {
                    guiGraphics.drawString(font, line, 12, textDrawY, COLOR_TEXT_HIGHLIGHT, false);
                    textDrawY += 14;
                }
                renderedAnyLine = true;
            }

            if (!renderedAnyLine && currentTab == 1 && !playerSellableItems.isEmpty()) {
                ItemStack stack = playerSellableItems.get(selectedIndex);
                String fallback = I18n.get(stack.getDescriptionId());
                List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(fallback), this.width - 24);
                for (net.minecraft.util.FormattedCharSequence line : lines) {
                    guiGraphics.drawString(font, line, 12, textDrawY, COLOR_TEXT_HIGHLIGHT, false);
                    textDrawY += 14;
                }
            }
        }
    }

    private void drawItemDetails(GuiGraphics guiGraphics, long count, int listY) {
        Component ownedTxt = Component.translatable("gui.blacksouls.shop.owned");
        String countTxt = String.valueOf(count);
        int textY = listY + 15;
        guiGraphics.drawString(font, ownedTxt, listW + 15, textY, COLOR_TITLE_BLUE, false);
        guiGraphics.drawString(font, countTxt, this.width - 20 - font.width(countTxt), textY, COLOR_TEXT_HIGHLIGHT, false);
    }

    private void drawBuyItemList(GuiGraphics guiGraphics, int listStartX, int listStartY) {
        for (int i = 0; i < shopItems.size(); i++) {
            if (isSelectingQuantity && i != selectedIndex) continue;

            ShopItem item = shopItems.get(i);
            int rowY = listStartY + (isSelectingQuantity ? 0 : (i * 22));

            if (i == selectedIndex && !isSelectingQuantity) {
                guiGraphics.fill(listStartX, rowY - 3, listW - 8, rowY + 18, 0x66FFFFFF);
            }

            ItemStack renderStack = new ItemStack(item.itemReg.get());
            ResourceLocation itemTex = new ResourceLocation(BlackSouls.MODID, "textures/item/" + item.itemReg.getId().getPath() + ".png");

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(itemTex, listStartX + 2, rowY, 16, 16, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();

            guiGraphics.drawString(font, I18n.get(renderStack.getDescriptionId()), listStartX + 22, rowY + 3, COLOR_TEXT_HIGHLIGHT, false);

            if (isSelectingQuantity && i == selectedIndex) {
                drawQuantityBox(guiGraphics, rowY, item.price);
            } else {
                String priceTxt = String.valueOf(item.price);
                guiGraphics.drawString(font, priceTxt, listW - 12 - font.width(priceTxt), rowY + 3, COLOR_TEXT_HIGHLIGHT, false);
            }
        }
    }

    private void drawSellPlayerInventoryList(GuiGraphics guiGraphics, int listStartX, int listStartY) {
        if (this.minecraft.player == null) return;
        int columns = 2;
        int itemWidth = this.width / 2;
        int itemHeight = 22;

        for (int i = 0; i < playerSellableItems.size(); i++) {
            if (isSelectingQuantity && i != selectedIndex) continue;

            ItemStack stack = playerSellableItems.get(i);
            Item item = stack.getItem();
            BSItemSellRegistry.SellInfo info = BSItemSellRegistry.SELL_PRICES.get(item);
            if (info == null) continue;

            int col = isSelectingQuantity ? 0 : (i % columns);
            int row = isSelectingQuantity ? 0 : (i / columns);

            int cellX = isSelectingQuantity ? listStartX : (col * itemWidth);
            int rowY = listStartY + row * itemHeight;
            int currentItemW = isSelectingQuantity ? (listW - 8) : itemWidth;

            if (i == selectedIndex && !isSelectingQuantity) {
                guiGraphics.fill(cellX + 6, rowY - 3, cellX + currentItemW - 6, rowY + 18, 0x66FFFFFF);
            }

            int iconOffset = isSelectingQuantity ? 2 : 12;
            guiGraphics.renderItem(stack, cellX + iconOffset, rowY);
            guiGraphics.drawString(font, I18n.get(stack.getDescriptionId()), cellX + iconOffset + 20, rowY + 3, COLOR_TEXT_HIGHLIGHT, false);

            if (isSelectingQuantity && i == selectedIndex) {
                drawQuantityBox(guiGraphics, rowY, info.price);
            } else {
                String countTxt = ": " + getInventoryCountFor(this.minecraft.player, stack);
                guiGraphics.drawString(font, countTxt, cellX + currentItemW - font.width(countTxt) - 16, rowY + 3, COLOR_TEXT_HIGHLIGHT, false);
            }
        }
    }

    private void drawQuantityBox(GuiGraphics guiGraphics, int rowY, long unitPrice) {
        int boxY = rowY + 24;
        int boxX = listW - 50;
        guiGraphics.drawString(font, "x", boxX - 15, boxY + 4, COLOR_TEXT_HIGHLIGHT, false);

        int alpha = (int) (60 + 40 * Math.sin(System.currentTimeMillis() / 150.0));
        guiGraphics.fill(boxX, boxY, boxX + 40, boxY + 16, (alpha << 24) | 0xFFFFFF);
        guiGraphics.drawCenteredString(font, String.valueOf(actionQuantity), boxX + 20, boxY + 4, COLOR_TEXT_HIGHLIGHT);

        String totalStr = String.valueOf(unitPrice * actionQuantity);
        int totalX = listW - 15 - font.width(totalStr) - font.width(" S");
        guiGraphics.drawString(font, totalStr, totalX, boxY + 24, COLOR_TEXT_HIGHLIGHT, false);
        guiGraphics.drawString(font, " S", totalX + font.width(totalStr), boxY + 24, COLOR_S_SUFFIX, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int tabCenterY = descH + (tabH / 2) - 4;
        if (mouseY >= tabCenterY - 6 && mouseY <= tabCenterY + 14) {
            int buyTabX = (int) (this.width * 0.15f);
            int sellTabX = (int) (this.width * 0.35f);
            int cancelTabX = (int) (this.width * 0.55f);

            if (mouseX >= buyTabX - 5 && mouseX <= buyTabX + 50) { switchTab(0); return true; }
            if (mouseX >= sellTabX - 5 && mouseX <= sellTabX + 50) { switchTab(1); return true; }
            if (mouseX >= cancelTabX - 10 && mouseX <= cancelTabX + font.width(Component.translatable("gui.blacksouls.shop.tab.cancel")) + 10) {
                playCursorSound(); this.onClose(); return true;
            }
        }

        int currentListY = descH + tabH;
        if (currentTab == 1 && mouseY >= currentListY && mouseY <= currentListY + subTabH) {
            int sectionWidth = this.width / 4;
            for (int i = 0; i < 4; i++) {
                if (mouseX >= i * sectionWidth && mouseX <= (i + 1) * sectionWidth) {
                    playCursorSound();
                    currentSubTab = i;
                    selectedIndex = 0;
                    isSelectingQuantity = false;
                    updatePlayerInventoryList();
                    return true;
                }
            }
        }
        if (currentTab == 1) currentListY += subTabH;

        int listStartX = 8;
        int listStartY = currentListY + 8;
        boolean clickedOnItem = false;

        int columns = (currentTab == 0) ? 1 : 2;
        int listSize = (currentTab == 0 ? shopItems.size() : playerSellableItems.size());
        int itemWidth = (currentTab == 0) ? listW : (this.width / 2);

        for (int i = 0; i < listSize; i++) {
            if (isSelectingQuantity && i != selectedIndex) continue;

            int col = isSelectingQuantity ? 0 : (i % columns);
            int row = isSelectingQuantity ? 0 : (i / columns);
            int cellX = (currentTab == 0 || isSelectingQuantity) ? listStartX : (col * itemWidth);
            int rowY = listStartY + row * 22;
            int currentItemW = (currentTab == 0) ? (listW - 16) : (isSelectingQuantity ? (listW - 8) : itemWidth);
            int clickHeight = (isSelectingQuantity && i == selectedIndex) ? 40 : 18;

            if (mouseX >= cellX && mouseX <= cellX + currentItemW && mouseY >= rowY - 3 && mouseY <= rowY + clickHeight) {
                clickedOnItem = true;
                if (selectedIndex == i) {
                    if (!isSelectingQuantity) {
                        isSelectingQuantity = true;
                        actionQuantity = 1;
                        playCursorSound();
                    } else {
                        if (currentTab == 0) buySelectedItem(); else sellSelectedItem();
                    }
                } else {
                    selectedIndex = i;
                    isSelectingQuantity = false;
                    playCursorSound();
                    updateSelectionState();
                }
                return true;
            }
        }

        if (isSelectingQuantity && !clickedOnItem) {
            if (mouseX >= 0 && mouseX <= (currentTab == 0 ? listW : this.width) && mouseY >= currentListY && mouseY <= currentListY + baseListH) {
                isSelectingQuantity = false;
                playCursorSound();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchTab(int tabIndex) {
        playCursorSound();
        currentTab = tabIndex;
        if (tabIndex == 0) currentSubTab = 0;
        selectedIndex = 0;
        isSelectingQuantity = false;
        updatePlayerInventoryList();
    }

    private void buySelectedItem() {
        if (shopItems.isEmpty()) return;
        ShopItem item = shopItems.get(selectedIndex);
        long totalCost = item.price * actionQuantity;
        if (currentSouls >= totalCost) {
            NetworkHandler.sendToServer(new ServerboundTradePacket(ServerboundTradePacket.Action.BUY, item.itemReg.getId().toString(), actionQuantity));
            isSelectingQuantity = false;
            playCursorSound();
        } else {
            if (this.minecraft.player != null) {
                this.minecraft.player.level().playSound(null, this.minecraft.player.blockPosition(), BlackSouls.CURSOR1_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F);
            }
        }
    }

    private void sellSelectedItem() {
        if (playerSellableItems.isEmpty() || this.minecraft.player == null) return;
        ItemStack stackToSell = playerSellableItems.get(selectedIndex);
        long actualOwned = getInventoryCountFor(this.minecraft.player, stackToSell);

        if (actualOwned >= actionQuantity) {
            String itemRL = stackToSell.getItem().builtInRegistryHolder().key().location().toString();
            NetworkHandler.sendToServer(new ServerboundTradePacket(ServerboundTradePacket.Action.SELL, itemRL, actionQuantity));

            isSelectingQuantity = false;
            playCursorSound();
            updatePlayerInventoryList();
        } else {
            this.minecraft.player.level().playSound(null, this.minecraft.player.blockPosition(), BlackSouls.CURSOR1_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F);
        }
    }

    private void playCursorSound() {
        if (BlackSouls.CURSOR1_EVENT != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            if (isSelectingQuantity) {
                isSelectingQuantity = false;
                playCursorSound();
                return true;
            }
            playCursorSound();
            this.onClose();
            return true;
        }

        if (isSelectingQuantity) {
            if (keyCode == InputConstants.KEY_RIGHT) { actionQuantity += 1; clampQuantity(); playCursorSound(); return true; }
            if (keyCode == InputConstants.KEY_LEFT)  { actionQuantity -= 1; clampQuantity(); playCursorSound(); return true; }
            if (keyCode == InputConstants.KEY_UP)    { actionQuantity += 11; clampQuantity(); playCursorSound(); return true; }
            if (keyCode == InputConstants.KEY_DOWN)  { actionQuantity -= 11; clampQuantity(); playCursorSound(); return true; }
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                if (currentTab == 0) buySelectedItem(); else sellSelectedItem();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            if ((currentTab == 0 && !shopItems.isEmpty()) || (currentTab == 1 && !playerSellableItems.isEmpty())) {
                isSelectingQuantity = true;
                actionQuantity = 1;
                playCursorSound();
                return true;
            }
        }

        boolean selectionChanged = false;
        if (keyCode == InputConstants.KEY_RIGHT || keyCode == InputConstants.KEY_PAGEDOWN) {
            if (currentTab < 1) { switchTab(1); return true; }
        } else if (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_PAGEUP) {
            if (currentTab > 0) { switchTab(0); return true; }
        } else if (keyCode == InputConstants.KEY_UP) {
            if (selectedIndex > 0) { selectedIndex--; selectionChanged = true; }
        } else if (keyCode == InputConstants.KEY_DOWN) {
            int maxIndex = (currentTab == 0 ? shopItems.size() : playerSellableItems.size()) - 1;
            if (selectedIndex < maxIndex) { selectedIndex++; selectionChanged = true; }
        }

        if (selectionChanged) {
            playCursorSound();
            updateSelectionState();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void clampQuantity() {
        if (currentTab == 1 && this.minecraft.player != null && !playerSellableItems.isEmpty()) {
            long actualOwned = getInventoryCountFor(this.minecraft.player, playerSellableItems.get(selectedIndex));
            actionQuantity = (int) Math.max(1, Math.min(actualOwned, actionQuantity));
        } else {
            actionQuantity = Math.max(1, Math.min(999, actionQuantity));
        }
    }

    private int getItemSubTabType(ItemStack stack) {
        Item item = stack.getItem();
        ResourceLocation registryName = item.builtInRegistryHolder().key().location();
        String path = registryName.getPath();

        
        if (path.contains("essential") || 
            path.contains("skill_book") || 
            path.contains("covenant") || 
            path.contains("book_") || 
            path.contains("dev_")) {
            return 3;
        }

        
        if (item instanceof net.minecraft.world.item.ArmorItem || 
            path.contains("ring_") || 
            path.contains("armor") || 
            path.contains("helmet") || 
            path.contains("hat") || 
            path.contains("mask") || 
            path.contains("cloak") || 
            path.contains("clothes") || 
            path.contains("uniform") || 
            path.contains("attire") || 
            path.contains("ears") || 
            path.contains("hairband") || 
            path.contains("circlet") || 
            path.contains("vestment") ||
            path.contains("raiment")) {
            return 2;
        }
        
        if (item instanceof net.minecraft.world.item.TieredItem || 
            item instanceof net.minecraft.world.item.ProjectileWeaponItem || 
            path.contains("sword") || 
            path.contains("blade") || 
            path.contains("shotgun")) {
            return 1;
        }
        
        return 0;
    }

    private long getInventoryCountFor(Player player, ItemStack target) {
        long count = 0;
        Item targetItem = target.getItem();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(targetItem)) count += stack.getCount();
        }
        return count;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
