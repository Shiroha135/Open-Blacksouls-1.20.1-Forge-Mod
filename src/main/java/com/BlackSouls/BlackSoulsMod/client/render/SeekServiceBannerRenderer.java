package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiTurnBattle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public class SeekServiceBannerRenderer {
    private static final ResourceLocation ICON = new ResourceLocation(BlackSouls.MODID, "textures/gui/sendam.png");
    private static int ticksLeft = 0;
    private static int senDelta = 0;
    private static String deltaText = "0";
    private static int pendingSenDelta = 0;
    private static boolean pendingAfterBattle = false;

    public static void show(int delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GuiTurnBattle) {
            pendingSenDelta = clampDelta((long) pendingSenDelta + delta);
            pendingAfterBattle = true;
            ticksLeft = 0;
            return;
        }
        if (pendingAfterBattle) {
            delta = clampDelta((long) pendingSenDelta + delta);
            pendingAfterBattle = false;
            pendingSenDelta = 0;
        }
        start(delta);
    }

    private static void start(int delta) {
        TextBannerRenderer.hide();
        senDelta = delta;
        deltaText = (delta > 0 ? "+" : "") + delta;
        ticksLeft = 60;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (pendingAfterBattle) {
            if (minecraft.player == null) {
                pendingAfterBattle = false;
                pendingSenDelta = 0;
            } else if (!(minecraft.screen instanceof GuiTurnBattle)) {
                int delta = pendingSenDelta;
                pendingAfterBattle = false;
                pendingSenDelta = 0;
                start(delta);
            }
        }
        if (ticksLeft > 0 && !minecraft.isPaused()) {
            ticksLeft--;
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (ticksLeft <= 0 || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        BSPlayerStats stats = mc.player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        int barHeight = mc.font.lineHeight * 2 + 12;
        int barY = screenHeight - barHeight - lowerBannerHeight();
        int titleY = barY + 3;
        int valueY = barY + mc.font.lineHeight + 5;

        String titleKey = senDelta >= 0
                ? "gui.blacksouls.seek_service.banner.sen_up"
                : "gui.blacksouls.seek_service.banner.sen_down";
        String titleText = I18n.get(titleKey);
        String valueText = I18n.get(
                "gui.blacksouls.seek_service.banner.current_value",
                stats.sen,
                deltaText
        );
        int titleWidth = mc.font.width(titleText) + 8;
        guiGraphics.fill(0, barY, titleWidth, valueY - 2, 0xBB000000);
        guiGraphics.fill(0, valueY - 2, screenWidth, screenHeight - lowerBannerHeight(), 0xBB000000);
        guiGraphics.drawString(mc.font, titleText, 4, titleY, 0xFFFFFFFF, false);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(ICON, 4, valueY - 1, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.drawString(mc.font, valueText, 22, valueY, 0x55FF55, false);
    }

    private static int lowerBannerHeight() {
        int height = SoulGainBannerRenderer.isVisible() ? SoulGainBannerRenderer.getReservedHeight() : 0;
        try {
            Class<?> overlay = Class.forName(
                    "cn.zhenhongliya.blacksouls2compat.client.AcquisitionRewardOverlay"
            );
            Object value = overlay.getMethod("getReservedHeight").invoke(null);
            if (value instanceof Integer reservedHeight) {
                height = Math.max(height, reservedHeight);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return height;
    }

    private static int clampDelta(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }
}
