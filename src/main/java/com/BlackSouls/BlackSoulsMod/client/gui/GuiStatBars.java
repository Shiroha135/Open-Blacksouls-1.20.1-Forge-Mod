package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.client.render.ShaderHelper;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import com.BlackSouls.BlackSoulsMod.client.ClientSkillInfo;
import com.BlackSouls.BlackSoulsMod.client.render.BSAvatarRenderer;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public class GuiStatBars {
    private static float displayHp = -1.0f;
    private static float displayMp = -1.0f;
    private static double displayAction = -1.0;
    private static float displayFood = -1.0f;
    private static long lastHitTime = 0L;

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()
                || event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()
                || event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        float partialTicks = mc.getFrameTime();
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            float targetHp = player.getHealth();
            float maxHp = player.getMaxHealth();
            float targetMp = stats.getMana();
            float maxMp = stats.getMaxMana();
            double targetAction = SkillUtils.getCurrentActionPoints(player);
            double maxAction = SkillUtils.getMaxActionPoints(player);
            float targetFood = player.getFoodData().getFoodLevel();
            float maxFood = 20.0f;
            long currentTime = Util.getMillis();

            if (displayHp < 0.0f) {
                displayHp = targetHp;
            }
            if (displayMp < 0.0f) {
                displayMp = targetMp;
            }
            if (displayAction < 0.0) {
                displayAction = targetAction;
            }
            if (displayFood < 0.0f) {
                displayFood = targetFood;
            }

            if (displayHp > targetHp) {
                if (displayHp - targetHp > 1.0f) {
                    lastHitTime = Util.getMillis();
                }
                displayHp += (targetHp - displayHp) * 0.05f * partialTicks;
            } else {
                displayHp = targetHp;
            }

            displayMp = smoothStep(displayMp, targetMp, partialTicks, displayMp > targetMp ? 0.10f : 0.18f);
            displayAction = smoothStep(displayAction, targetAction, partialTicks, displayAction > targetAction ? 0.14 : 0.24);
            displayFood = smoothStep(displayFood, targetFood, partialTicks, displayFood > targetFood ? 0.12f : 0.22f);

            float realHpRatio = maxHp > 0.0f ? clampRatio(targetHp / maxHp) : 0.0f;
            float lerpHpRatio = maxHp > 0.0f ? clampRatio(displayHp / maxHp) : 0.0f;
            float realMpRatio = maxMp > 0.0f ? clampRatio(targetMp / maxMp) : 0.0f;
            float lerpMpRatio = maxMp > 0.0f ? clampRatio(displayMp / maxMp) : 0.0f;
            float realActionRatio = maxAction > 0.0 ? clampRatio((float) (targetAction / maxAction)) : 0.0f;
            float lerpActionRatio = maxAction > 0.0 ? clampRatio((float) (displayAction / maxAction)) : 0.0f;
            float realFoodRatio = maxFood > 0.0f ? clampRatio(targetFood / maxFood) : 0.0f;
            float lerpFoodRatio = maxFood > 0.0f ? clampRatio(displayFood / maxFood) : 0.0f;

            int avatarX = 14;
            int avatarY = 14;
            int avatarSize = 42;

            renderHudAvatar(graphics, avatarX, avatarY, avatarSize, realHpRatio, realMpRatio, currentTime);

            int x = avatarX + avatarSize + 8;
            int y = 14;
            int topWidth = 165;
            int subWidth = 61;
            int h = 8;
            int gap = 12;
            int subX = x;

            int hpY = y;
            int mpY = y + gap;
            int actionY = y + gap * 2;
            int foodY = y + gap * 3;

            boolean isRgbMode = maxHp >= 1099998.0f;

            renderBar(
                    graphics,
                    mc,
                    currentTime,
                    x,
                    hpY,
                    topWidth,
                    h,
                    realHpRatio,
                    lerpHpRatio,
                    isRgbMode ? 0.05f : 0.90f,
                    isRgbMode ? 1.00f : 0.12f,
                    isRgbMode ? 0.05f : 0.12f,
                    String.format(Locale.ROOT, "%d/%d", (int) targetHp, (int) maxHp),
                    isRgbMode ? getRainbowColor(currentTime) : 0xFFFFFF,
                    timeSinceLastHitNeedsShake(currentTime),
                    !isRgbMode && realHpRatio < 0.20f,
                    x + topWidth / 2,
                    hpY + h / 2
            );

            renderBar(
                    graphics,
                    mc,
                    currentTime,
                    x,
                    mpY,
                    topWidth,
                    h,
                    realMpRatio,
                    lerpMpRatio,
                    0.10f,
                    0.48f,
                    1.00f,
                    String.format(Locale.ROOT, "%d/%d", (int) targetMp, (int) maxMp),
                    0x33CCFF,
                    false,
                    realMpRatio < 0.20f,
                    x + topWidth / 2,
                    mpY + h / 2
            );

            renderBar(
                    graphics,
                    mc,
                    currentTime,
                    subX,
                    actionY,
                    subWidth,
                    h,
                    realActionRatio,
                    lerpActionRatio,
                    0.18f,
                    0.95f,
                    0.25f,
                    String.format(Locale.ROOT, "%.2f/%.2f", targetAction, maxAction),
                    0x66FF88,
                    false,
                    realActionRatio < 0.20f,
                    subX + subWidth / 2,
                    actionY + h / 2
            );

            renderBar(
                    graphics,
                    mc,
                    currentTime,
                    subX,
                    foodY,
                    subWidth,
                    h,
                    realFoodRatio,
                    lerpFoodRatio,
                    1.00f,
                    0.55f,
                    0.10f,
                    String.format(Locale.ROOT, "%d/%d", (int) targetFood, (int) maxFood),
                    0xFFB347,
                    false,
                    realFoodRatio < 0.20f,
                    subX + subWidth / 2,
                    foodY + h / 2
            );

        });
    }

    private static boolean timeSinceLastHitNeedsShake(long currentTime) {
        return currentTime - lastHitTime < 300L;
    }

    private static float smoothStep(float current, float target, float partialTicks, float speed) {
        float next = current + (target - current) * speed * partialTicks;
        return Math.abs(next - target) < 0.01f ? target : next;
    }

    private static double smoothStep(double current, double target, float partialTicks, double speed) {
        double next = current + (target - current) * speed * partialTicks;
        return Math.abs(next - target) < 0.001 ? target : next;
    }

    private static float clampRatio(float ratio) {
        return Math.max(0.0f, Math.min(1.0f, ratio));
    }

    private static void renderBar(
            GuiGraphics graphics,
            Minecraft mc,
            long currentTime,
            int x,
            int y,
            int width,
            int height,
            float realRatio,
            float lerpRatio,
            float red,
            float green,
            float blue,
            String text,
            int textColor,
            boolean hitShake,
            boolean lowPulse,
            int centerX,
            int centerY
    ) {
        graphics.pose().pushPose();

        if (hitShake) {
            apply3DShake(graphics, currentTime, 2.0f, centerX, centerY);
        } else if (lowPulse) {
            float throb = (float) Math.sin(currentTime / 120.0) * 0.5f + 0.5f;
            apply3DShake(graphics, currentTime, throb * 0.55f, centerX, centerY);
        }

        drawPixelBorderAndBg(graphics, x, y, width, height);

        if (lerpRatio > realRatio) {
            int trailingColor = (0x88 << 24) | (textColor & 0x00FFFFFF);
            graphics.fill(x, y, x + (int) (width * lerpRatio), y + height, trailingColor);
        }

        Matrix4f matrix = graphics.pose().last().pose();
        if (ShaderHelper.flowBarShader != null) {
            float smoothTime = (currentTime % 1_000_000L) / 1000.0f;
            if (ShaderHelper.flowBarShader.safeGetUniform("GameTime") != null) {
                ShaderHelper.flowBarShader.safeGetUniform("GameTime").set(smoothTime);
            }
            RenderSystem.setShader(() -> ShaderHelper.flowBarShader);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            float pulseAlpha = 0.82f + 0.18f * (float) Math.sin(currentTime / 200.0);
            drawShaderQuad(buffer, matrix, x, y, (int) (width * realRatio), height, red, green, blue, pulseAlpha);
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
        }

        graphics.drawString(mc.font, text, x + 4, y - 1, textColor, true);
        graphics.pose().popPose();
    }

    private static void apply3DShake(GuiGraphics graphics, long time, float intensity, int centerX, int centerY) {
        float rotation = (float) Math.sin(time / 20.0) * 2.0f * intensity;
        float scale = 1.0f + (float) Math.sin(time / 15.0) * 0.04f * intensity;
        float offsetX = (float) Math.cos(time / 10.0) * 1.5f * intensity;
        float offsetY = (float) Math.sin(time / 12.0) * 1.5f * intensity;

        graphics.pose().translate(centerX + offsetX, centerY + offsetY, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        graphics.pose().translate(-centerX, -centerY, 0.0f);
    }

    private static int getRainbowColor(long time) {
        float hue = (time % 2500L) / 2500.0f;
        return Color.HSBtoRGB(hue, 0.6f, 1.0f);
    }

    private static void drawPixelBorderAndBg(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xCC050505);
        graphics.fill(x, y, x + width, y + 1, 0x44FFFFFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0x44000000);

        int borderColor = 0xFF1A1A1A;
        graphics.fill(x, y - 1, x + width, y, borderColor);
        graphics.fill(x, y + height, x + width, y + height + 1, borderColor);
        graphics.fill(x - 1, y, x, y + height, borderColor);
        graphics.fill(x + width, y, x + width + 1, y + height, borderColor);
    }

    private static void drawShaderQuad(
            BufferBuilder buffer,
            Matrix4f matrix,
            int x,
            int y,
            int width,
            int height,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        if (width <= 0) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0f).uv(0.0f, 1.0f).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0.0f).uv(1.0f, 1.0f).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y, 0.0f).uv(1.0f, 0.0f).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y, 0.0f).uv(0.0f, 0.0f).color(red, green, blue, alpha).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }

    private static void renderHudAvatar(
            GuiGraphics graphics,
            int x,
            int y,
            int size,
            float hpRatio,
            float mpRatio,
            long currentTime
    ) {
        Minecraft mc = Minecraft.getInstance();
        String avatarName = ClientSkillInfo.getAvatar() != null ? ClientSkillInfo.getAvatar() : "knight_face";

        ResourceLocation tex = new ResourceLocation(
                BlackSouls.MODID,
                "textures/gui/avatars/" + avatarName + ".png"
        );

        graphics.pose().pushPose();

        if (hpRatio <= 0.25f) {
            float pulse = 1.0f + (float) Math.sin(currentTime / 90.0) * 0.06f;
            graphics.pose().translate(x + size / 2.0f, y + size / 2.0f, 0.0f);
            graphics.pose().scale(pulse, pulse, 1.0f);
            graphics.pose().translate(-(x + size / 2.0f), -(y + size / 2.0f), 0.0f);
        }

        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xAA050505);
        graphics.fill(x - 3, y - 3, x + size + 3, y - 2, 0xFF222222);
        graphics.fill(x - 3, y + size + 2, x + size + 3, y + size + 3, 0xFF000000);
        graphics.fill(x - 3, y - 2, x - 2, y + size + 2, 0xFF222222);
        graphics.fill(x + size + 2, y - 2, x + size + 3, y + size + 2, 0xFF000000);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BSAvatarRenderer.draw(graphics, tex, avatarName, x, y, size);
        RenderSystem.disableBlend();

        graphics.pose().popPose();
    }

}
