package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketClaimPurgeTaskReward;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundSimpleActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class GuiPurgeCommission extends Screen {
    private static final int PANEL_WIDTH = 404;
    private static final int PANEL_MAX_HEIGHT = 348;
    private static final int PANEL_MIN_HEIGHT = 212;
    private static final long REFRESH_TICKS = 12000L;
    private static final int TASK_HEIGHT = 56;
    private static final int TASK_SPACING = 6;
    private static final int REFRESH_BUTTON_WIDTH = 72;
    private static final int BUTTON_HEIGHT = 18;
    private static final int MAX_REWARD_CACHE_SIZE = 32;
    private int scrollOffset = 0;
    private long cachedRemainingSeconds = Long.MIN_VALUE;
    private String cachedRemainingTime = "--:--";
    private final Map<String, Item> rewardItemCache = new HashMap<>();

    public GuiPurgeCommission() {
        super(Component.translatable("gui.blacksouls.purge.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.scrollOffset = 0;
        this.rewardItemCache.clear();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int bottom = top + panelHeight;
        int refreshButtonLeft = left + panelWidth - REFRESH_BUTTON_WIDTH - 14;
        int refreshButtonTop = top + 46;

        BSPlayerStats stats = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).orElse(null)
                : null;

        BSGuiUtils.drawRMWindow(guiGraphics, left, top, panelWidth, panelHeight);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.blacksouls.purge.title"), this.width / 2, top + 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.blacksouls.purge.total_completed", stats != null ? stats.purgeTrashEarned : 0), left + 14, top + 30, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.blacksouls.purge.refresh", formatRemainingTicks()), left + 14, top + 48, 0x55AAFF, false);

        boolean refreshHovered = isInside(mouseX, mouseY, refreshButtonLeft, refreshButtonTop, REFRESH_BUTTON_WIDTH, BUTTON_HEIGHT);
        BSGuiUtils.drawRMWindow(guiGraphics, refreshButtonLeft, refreshButtonTop, REFRESH_BUTTON_WIDTH, BUTTON_HEIGHT);
        if (refreshHovered) {
            guiGraphics.fill(refreshButtonLeft + 4, refreshButtonTop + 3, refreshButtonLeft + REFRESH_BUTTON_WIDTH - 4, refreshButtonTop + BUTTON_HEIGHT - 3, 0x33FFFFFF);
        }
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.blacksouls.purge.refresh_button"), refreshButtonLeft + REFRESH_BUTTON_WIDTH / 2, refreshButtonTop + 5, 0xFFFFFF);

        if (stats == null || stats.purgeTasks.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.blacksouls.purge.empty"), this.width / 2, top + 122, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            return;
        }

        clampScrollOffset(stats);
        guiGraphics.drawString(this.font, Component.translatable("gui.blacksouls.purge.refresh_count", Math.max(0, 5 - stats.purgeRefreshesUsedToday), 5), left + 14, top + 60, 0xFFFFFF, false);

        int currentY = top + 82;
        int visibleTaskCount = getVisibleTaskCount(panelHeight);
        int visibleTasks = Math.min(visibleTaskCount, stats.purgeTasks.size() - this.scrollOffset);
        for (int i = 0; i < visibleTasks; i++) {
            BSPlayerStats.PurgeCommissionTask task = stats.purgeTasks.get(i + this.scrollOffset);
            renderTask(guiGraphics, mouseX, mouseY, left + 12, currentY, panelWidth - 24, TASK_HEIGHT, task, this.scrollOffset + i + 1);
            currentY += TASK_HEIGHT + TASK_SPACING;
        }

        if (stats.purgeTasks.size() > visibleTaskCount) {
            String pageText = (this.scrollOffset + 1) + "-" + (this.scrollOffset + visibleTasks) + " / " + stats.purgeTasks.size();
            guiGraphics.drawCenteredString(this.font, Component.literal(pageText), this.width / 2, bottom - 18, 0xAAAAAA);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderTask(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int width, int height, BSPlayerStats.PurgeCommissionTask task, int index) {
        BSGuiUtils.drawRMWindow(guiGraphics, left, top, width, height);

        Component category = Component.translatable("gui.blacksouls.purge.category." + task.category);
        Component target = Component.translatable("gui.blacksouls.purge.target." + task.category + "." + task.targetId);
        String progressText = task.progress + "/" + task.required;
        Item rewardItem = getRewardItem(task.rewardItemId);
        String rewardText = resolveRewardName(rewardItem, task.rewardItemId) + " x" + task.rewardCount;
        int buttonLeft = left + width - 70;
        int buttonTop = top + height - 22;

        guiGraphics.drawString(this.font, Component.translatable("gui.blacksouls.purge.index", index).append(" ").append(category), left + 10, top + 7, 0x55AAFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.blacksouls.purge.task_line", target, progressText), left + 10, top + 19, 0xFFFFFF, false);
        Component rewardLabel = Component.translatable("gui.blacksouls.purge.reward_label");
        int rewardLabelX = left + 10;
        int rewardY = top + 35;
        guiGraphics.drawString(this.font, rewardLabel, rewardLabelX, rewardY, 0xC8C8C8, false);
        int rewardIconX = rewardLabelX + this.font.width(rewardLabel) + 4;
        if (rewardItem != null) {
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(rewardItem), rewardIconX, top + 31);
        }
        drawWrapped(guiGraphics, Component.literal(rewardText), rewardIconX + 20, rewardY, width - (rewardIconX - left) - 92, 0xC8C8C8, 10);

        if (task.rewarded) {
            guiGraphics.drawString(this.font, Component.translatable("gui.blacksouls.purge.completed"), left + width - 10 - this.font.width(Component.translatable("gui.blacksouls.purge.completed")), top + 7, 0x77FF77, false);
        } else if (task.isComplete()) {
            boolean hovered = isInside(mouseX, mouseY, buttonLeft, buttonTop, 58, BUTTON_HEIGHT);
            BSGuiUtils.drawRMWindow(guiGraphics, buttonLeft, buttonTop, 58, BUTTON_HEIGHT);
            if (hovered) {
                guiGraphics.fill(buttonLeft + 4, buttonTop + 3, buttonLeft + 54, buttonTop + BUTTON_HEIGHT - 3, 0x33FFFFFF);
            }
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.blacksouls.purge.claim_button"), buttonLeft + 29, buttonTop + 5, 0xFFFFFF);
        }
    }

    private void drawWrapped(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color, int lineHeight) {
        int currentY = y;
        for (var line : this.font.split(text, width)) {
            guiGraphics.drawString(this.font, line, x, currentY, color, false);
            currentY += lineHeight;
        }
    }

    private String resolveRewardName(Item rewardItem, String rewardItemId) {
        return rewardItem != null ? rewardItem.getDescription().getString() : rewardItemId;
    }

    private Item getRewardItem(String rewardItemId) {
        if (this.rewardItemCache.containsKey(rewardItemId)) {
            return this.rewardItemCache.get(rewardItemId);
        }
        if (this.rewardItemCache.size() >= MAX_REWARD_CACHE_SIZE) {
            this.rewardItemCache.clear();
        }
        Item rewardItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(rewardItemId));
        this.rewardItemCache.put(rewardItemId, rewardItem);
        return rewardItem;
    }

    private String formatRemainingTicks() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return "--:--";
        }

        long dayTime = this.minecraft.player.level().getDayTime();
        long remaining = REFRESH_TICKS - (dayTime % REFRESH_TICKS);
        long seconds = remaining / 20L;
        if (seconds == this.cachedRemainingSeconds) {
            return this.cachedRemainingTime;
        }
        long minutesPart = seconds / 60L;
        long secondsPart = seconds % 60L;
        this.cachedRemainingSeconds = seconds;
        this.cachedRemainingTime = String.format("%02d:%02d", minutesPart, secondsPart);
        return this.cachedRemainingTime;
    }

    private boolean isInside(double mouseX, double mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private int getPanelWidth() {
        return Math.min(PANEL_WIDTH, this.width - 40);
    }

    private int getPanelHeight() {
        return Math.max(PANEL_MIN_HEIGHT, Math.min(PANEL_MAX_HEIGHT, this.height - 40));
    }

    private int getVisibleTaskCount(int panelHeight) {
        int usableHeight = panelHeight - 108;
        return Math.max(1, usableHeight / (TASK_HEIGHT + TASK_SPACING));
    }

    private void clampScrollOffset(BSPlayerStats stats) {
        int maxOffset = Math.max(0, stats.purgeTasks.size() - getVisibleTaskCount(getPanelHeight()));
        if (this.scrollOffset > maxOffset) {
            this.scrollOffset = maxOffset;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int refreshButtonLeft = left + panelWidth - REFRESH_BUTTON_WIDTH - 14;
        int refreshButtonTop = top + 46;

        if (isInside(mouseX, mouseY, refreshButtonLeft, refreshButtonTop, REFRESH_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            NetworkHandler.sendToServer(new ServerboundSimpleActionPacket(ServerboundSimpleActionPacket.Action.REFRESH_PURGE_COMMISSIONS));
            return true;
        }

        BSPlayerStats stats = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getCapability(BSPlayerStats.CAPABILITY).orElse(null)
                : null;
        if (stats == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        clampScrollOffset(stats);
        int currentY = top + 82;
        int visibleTaskCount = getVisibleTaskCount(panelHeight);
        int visibleTasks = Math.min(visibleTaskCount, stats.purgeTasks.size() - this.scrollOffset);
        for (int i = 0; i < visibleTasks; i++) {
            BSPlayerStats.PurgeCommissionTask task = stats.purgeTasks.get(i + this.scrollOffset);
            int taskLeft = left + 12;
            int taskWidth = panelWidth - 24;
            int claimLeft = taskLeft + taskWidth - 70;
            int claimTop = currentY + TASK_HEIGHT - 22;
            if (!task.rewarded && task.isComplete() && isInside(mouseX, mouseY, claimLeft, claimTop, 58, BUTTON_HEIGHT)) {
                NetworkHandler.sendToServer(new PacketClaimPurgeTaskReward(i + this.scrollOffset));
                return true;
            }
            currentY += TASK_HEIGHT + TASK_SPACING;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        BSPlayerStats stats = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getCapability(BSPlayerStats.CAPABILITY).orElse(null)
                : null;
        if (stats == null) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        int visibleTaskCount = getVisibleTaskCount(getPanelHeight());
        if (stats.purgeTasks.size() <= visibleTaskCount) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        int maxOffset = Math.max(0, stats.purgeTasks.size() - visibleTaskCount);
        if (delta < 0 && this.scrollOffset < maxOffset) {
            this.scrollOffset++;
            return true;
        }
        if (delta > 0 && this.scrollOffset > 0) {
            this.scrollOffset--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
