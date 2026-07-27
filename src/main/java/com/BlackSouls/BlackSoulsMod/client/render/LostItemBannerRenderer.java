package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class LostItemBannerRenderer {
    private static ItemStack lost = ItemStack.EMPTY;
    private static int ticksLeft;

    public static void show(ItemStack stack) {
        lost = stack.copy();
        lost.setCount(Math.max(1, stack.getCount()));
        ticksLeft = 100;
        TextBannerRenderer.hide();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksLeft > 0 && !Minecraft.getInstance().isPaused()) {
            ticksLeft--;
            if (ticksLeft == 0) {
                lost = ItemStack.EMPTY;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (ticksLeft <= 0 || lost.isEmpty() || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int titleY = screenHeight - 48;
        int detailY = screenHeight - 28;
        Component title = Component.translatable("gui.blacksouls.item_lost");
        int titleWidth = minecraft.font.width(title) + 8;
        graphics.fill(0, titleY, titleWidth, detailY, 0xBB000000);
        graphics.fill(0, detailY, screenWidth, screenHeight, 0xC8000000);
        graphics.drawString(minecraft.font, title, 4, titleY + 5, 0xFFFFFFFF, false);
        graphics.renderItem(lost, 4, detailY + 3);
        Component label = lost.getHoverName().copy()
                .append(Component.literal(" x-" + lost.getCount()).withStyle(ChatFormatting.WHITE));
        graphics.drawString(minecraft.font, label, 24, detailY + 7, 0xFFFFFFFF, false);
        int descriptionX = 24 + minecraft.font.width(label) + 8;
        int available = screenWidth - descriptionX - 6;
        if (available > 12) {
            String description = firstDescriptionLine(minecraft, lost);
            if (!description.isEmpty()) {
                graphics.drawString(minecraft.font,
                        minecraft.font.plainSubstrByWidth(description, available),
                        descriptionX, detailY + 7, 0xFFE0E0E0, false);
            }
        }
    }

    private static String firstDescriptionLine(Minecraft minecraft, ItemStack stack) {
        List<Component> tooltip = stack.getTooltipLines(minecraft.player, TooltipFlag.Default.NORMAL);
        for (int index = 1; index < tooltip.size(); index++) {
            String line = tooltip.get(index).getString().trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
        return "";
    }

    private LostItemBannerRenderer() {
    }
}
