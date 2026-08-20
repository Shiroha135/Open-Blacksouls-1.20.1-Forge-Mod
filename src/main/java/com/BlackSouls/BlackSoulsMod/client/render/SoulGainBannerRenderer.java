package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
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
@SuppressWarnings("removal")
public class SoulGainBannerRenderer {
    private static final ResourceLocation ICON = new ResourceLocation(BlackSouls.MODID, "textures/item/consumable/soul_standard.png");

    private static int ticksLeft = 0;
    private static long soulDelta = 0L;
    private static String deltaText = "0S";

    public static void show(long delta) {
        TextBannerRenderer.hide();
        soulDelta = delta;
        deltaText = (delta > 0 ? "+" : "") + delta + "S";
        ticksLeft = 60;
    }

    public static boolean isVisible() {
        return ticksLeft > 0;
    }

    public static int getReservedHeight() {
        return Minecraft.getInstance().font.lineHeight * 2 + 12;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksLeft > 0 && !Minecraft.getInstance().isPaused()) {
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
        int barY = screenHeight - barHeight;
        int titleY = barY + 3;
        int valueY = barY + mc.font.lineHeight + 5;

        String titleKey = soulDelta >= 0
                ? "gui.blacksouls.soul.banner.soul_up"
                : "gui.blacksouls.soul.banner.soul_down";
        String titleText = I18n.get(titleKey);
        String valueText = I18n.get(
                "gui.blacksouls.soul.banner.current_value",
                stats.souls,
                deltaText
        );
        int titleWidth = mc.font.width(titleText) + 8;
        guiGraphics.fill(0, barY, titleWidth, valueY - 2, 0xBB000000);
        guiGraphics.fill(0, valueY - 2, screenWidth, screenHeight, 0xBB000000);
        guiGraphics.drawString(mc.font, titleText, 4, titleY, 0xFFFFFFFF, false);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(ICON, 4, valueY - 1, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int valueColor = soulDelta >= 0 ? 0xFFFF55 : 0xFF5555;
        guiGraphics.drawString(mc.font, valueText, 22, valueY, valueColor, false);
    }
}
