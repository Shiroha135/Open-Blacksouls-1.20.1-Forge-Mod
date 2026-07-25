package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class TextBannerRenderer {
    private static final int DURATION = 60;
    private static Component message = Component.empty();
    private static int ticksLeft;

    public static void show(Component text) {
        message = text.copy();
        ticksLeft = DURATION;
    }

    public static void hide() {
        ticksLeft = 0;
        message = Component.empty();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && ticksLeft > 0
                && !Minecraft.getInstance().isPaused()) {
            ticksLeft--;
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (ticksLeft <= 0 || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int bannerHeight = 90;
        int bottom = graphics.guiHeight();
        int top = bottom - bannerHeight;
        float opacity = Math.min(1.0F, Math.min((DURATION - ticksLeft + 1) / 6.0F, ticksLeft / 6.0F));
        FadedBannerRenderer.draw(graphics, 0, top, graphics.guiWidth(), bottom, opacity);
        int textColor = Math.round(255.0F * opacity) << 24 | 0xFFFFFF;
        graphics.drawString(
                minecraft.font,
                message,
                16,
                top + 16,
                textColor,
                true
        );
    }

    private TextBannerRenderer() {
    }
}
