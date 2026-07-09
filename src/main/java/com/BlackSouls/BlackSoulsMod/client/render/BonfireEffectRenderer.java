package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public class BonfireEffectRenderer {

    public static int whiteFlashTicks = 0;
    public static int darkOverlayTicks = 0;
    public static int bonfireLitTicks = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (whiteFlashTicks > 0) whiteFlashTicks--;
            if (darkOverlayTicks > 0) darkOverlayTicks--;
            if (bonfireLitTicks > 0) bonfireLitTicks--;
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().getPath().equals("title_text")) {
            if (darkOverlayTicks > 0) {
                GuiGraphics graphics = event.getGuiGraphics();
                int screenWidth = graphics.guiWidth();
                int screenHeight = graphics.guiHeight();
                int bannerHeight = (int)(screenHeight * 0.15F);
                int y1 = (int)(screenHeight * 0.33F);
                int y2 = y1 + bannerHeight;
                float alpha = 1.0f;
                if (darkOverlayTicks > 50) alpha = (60 - darkOverlayTicks) / 10.0f;
                else if (darkOverlayTicks < 10) alpha = darkOverlayTicks / 10.0f;
                int alphaInt = (int) (alpha * 170.0f);
                int color = alphaInt << 24;
                RenderSystem.enableBlend();
                graphics.fill(0, y1, screenWidth, y2, color);
                RenderSystem.disableBlend();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().getPath().equals("all") || event.getOverlay().id().getPath().equals("hotbar")) {

            if (whiteFlashTicks > 0) {
                GuiGraphics graphics = event.getGuiGraphics();
                float alpha = (float) whiteFlashTicks / 20.0f;
                int alphaInt = (int) (alpha * 255.0f);
                int color = (alphaInt << 24) | 0xFFFFFF;

                RenderSystem.enableBlend();
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
                RenderSystem.disableBlend();
            }

            if (bonfireLitTicks > 0) {
                GuiGraphics graphics = event.getGuiGraphics();
                Minecraft mc = Minecraft.getInstance();

                int screenWidth = graphics.guiWidth();
                int screenHeight = graphics.guiHeight();

                float alpha = 1.0f;
                if (bonfireLitTicks > 60) {
                    alpha = (80 - bonfireLitTicks) / 20.0f;
                } else if (bonfireLitTicks < 20) {
                    alpha = bonfireLitTicks / 20.0f;
                }

                int bannerHeight = (int)(screenHeight * 0.15F);
                int y1 = (int)(screenHeight * 0.33F);
                int y2 = y1 + bannerHeight;
                int bannerAlpha = (int) (alpha * 170.0f);
                int bannerColor = bannerAlpha << 24;

                RenderSystem.enableBlend();
                graphics.fill(0, y1, screenWidth, y2, bannerColor);

                int textAlpha = (int) (alpha * 255.0f);
                int textColor = (textAlpha << 24) | 0xFFAA00;
                Component text = Component.literal("BONFIRE LIT");

                graphics.pose().pushPose();

                graphics.pose().translate(0, 0, 100);

                float scale = 3.5F;
                graphics.pose().scale(scale, scale, 1.0F);

                int textX = (int) (screenWidth / 2.0F / scale);
                int textY = (int) ((y1 + (bannerHeight - mc.font.lineHeight * scale) / 2.0F) / scale);
                int textWidth = mc.font.width(text);

                graphics.drawString(mc.font, text, textX - textWidth / 2, textY, textColor, false);
                graphics.pose().popPose();

                RenderSystem.disableBlend();
            }
        }
    }
}