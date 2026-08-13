package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiTurnBattle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class TextBannerRenderer {
    private static final int DEFAULT_DURATION = 60;
    private static Component message = Component.empty();
    private static int ticksLeft;
    private static int activeDuration = DEFAULT_DURATION;
    private static int rgb = 0xFFFFFF;
    private static boolean centered;
    private static boolean waitingForBattleEnd;
    private static SoundEvent activationSound;

    public static void show(Component text) {
        message = text.copy();
        activeDuration = DEFAULT_DURATION;
        ticksLeft = activeDuration;
        rgb = 0xFFFFFF;
        centered = false;
        waitingForBattleEnd = false;
        activationSound = null;
    }

    public static void showCentered(Component text, int color, int duration, SoundEvent sound) {
        message = text.copy();
        activeDuration = Math.max(20, duration);
        ticksLeft = 0;
        rgb = color & 0xFFFFFF;
        centered = true;
        waitingForBattleEnd = true;
        activationSound = sound;
    }

    public static boolean isWaitingForCenteredBanner() {
        return centered && waitingForBattleEnd && !message.getString().isEmpty();
    }

    public static void hide() {
        ticksLeft = 0;
        message = Component.empty();
        waitingForBattleEnd = false;
        activationSound = null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (waitingForBattleEnd) {
            if (minecraft.screen instanceof GuiTurnBattle) {
                return;
            }
            waitingForBattleEnd = false;
            ticksLeft = activeDuration;
            if (activationSound != null) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(activationSound, 1.0F, 1.0F));
                activationSound = null;
            }
        } else if (ticksLeft > 0 && !minecraft.isPaused()) {
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
        int bannerHeight = centered
                ? Math.max(1, (int) (graphics.guiHeight() * 0.15F))
                : 90;
        int top = centered
                ? (int) (graphics.guiHeight() * 0.33F)
                : graphics.guiHeight() - bannerHeight;
        int bottom = top + bannerHeight;
        float opacity = Math.min(1.0F,
                Math.min((activeDuration - ticksLeft + 1) / 6.0F, ticksLeft / 6.0F));
        FadedBannerRenderer.draw(graphics, 0, top, graphics.guiWidth(), bottom, opacity);
        int textColor = Math.round(255.0F * opacity) << 24 | rgb;
        if (centered) {
            float scale = 3.5F;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 100.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            int textX = (int) (graphics.guiWidth() / 2.0F / scale)
                    - minecraft.font.width(message) / 2;
            int textY = (int) ((top
                    + (bannerHeight - minecraft.font.lineHeight * scale) / 2.0F) / scale);
            graphics.drawString(minecraft.font, message, textX, textY, textColor, false);
            graphics.pose().popPose();
        } else {
            graphics.drawString(minecraft.font, message, 16, top + 16, textColor, true);
        }
    }

    private TextBannerRenderer() {
    }
}
