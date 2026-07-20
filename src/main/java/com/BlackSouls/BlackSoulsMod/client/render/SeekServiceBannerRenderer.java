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
public class SeekServiceBannerRenderer {
    private static final ResourceLocation ICON = new ResourceLocation(BlackSouls.MODID, "textures/gui/sendam.png");
    private static int ticksLeft = 0;
    private static int senDelta = 0;
    private static String deltaText = "0";

    public static void show(int delta) {
        senDelta = delta;
        deltaText = (delta > 0 ? "+" : "") + delta;
        ticksLeft = 60;
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

        guiGraphics.fill(0, barY, screenWidth, screenHeight, 0xBB000000);

        String titleKey = senDelta >= 0
                ? "gui.blacksouls.seek_service.banner.sen_up"
                : "gui.blacksouls.seek_service.banner.sen_down";
        guiGraphics.drawString(mc.font, I18n.get(titleKey), 4, titleY, 0xFFFFFFFF, false);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(ICON, 4, valueY - 1, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        String valueText = I18n.get(
                "gui.blacksouls.seek_service.banner.current_value",
                stats.sen,
                deltaText
        );
        guiGraphics.drawString(mc.font, valueText, 22, valueY, 0x55FF55, false);
    }
}
