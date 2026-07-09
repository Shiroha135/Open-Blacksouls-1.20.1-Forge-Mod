package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public class LowSenJumpscareRenderer {
    private static final ResourceLocation JUMPSCARE_TEXTURE =
            new ResourceLocation(BlackSouls.MODID, "textures/gui/low_sen_jumpscare.png");

    private static int nextScareTicks = -1;
    private static int activeTicks = 0;
    private static float offsetX = 0.0F;
    private static float offsetY = 0.0F;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            reset();
            return;
        }

        if (!BSConfig.ENABLE_LOW_SEN_JUMPSCARE.get()) {
            reset();
            return;
        }

        BSPlayerStats stats = mc.player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null || stats.sen >= 30) {
            reset();
            return;
        }

        if (activeTicks > 0) {
            activeTicks--;
            return;
        }

        if (nextScareTicks < 0) {
            nextScareTicks = randomInterval(stats.sen);
            return;
        }

        if (nextScareTicks > 0) {
            nextScareTicks--;
            return;
        }

        triggerScare();
        nextScareTicks = randomInterval(stats.sen);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (activeTicks <= 0 || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        float alpha = activeTicks >= 4 ? 0.85F : 0.45F + activeTicks * 0.1F;
        int overlayAlpha = Math.min(255, Math.max(0, (int) (alpha * 140.0F)));
        guiGraphics.fill(0, 0, screenWidth, screenHeight, (overlayAlpha << 24));

        int drawX = Math.round(offsetX * 20.0F);
        int drawY = Math.round(offsetY * 15.0F);
        int drawWidth = screenWidth;
        int drawHeight = screenHeight;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(JUMPSCARE_TEXTURE, drawX, drawY, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void triggerScare() {
        activeTicks = 6;
        offsetX = ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F;
        offsetY = ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F;
    }

    private static int randomInterval(int sen) {
        int max = Math.max(120, 420 - Math.max(0, 29 - sen) * 8);
        int min = Math.max(40, max / 3);
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static void reset() {
        nextScareTicks = -1;
        activeTicks = 0;
    }
}
