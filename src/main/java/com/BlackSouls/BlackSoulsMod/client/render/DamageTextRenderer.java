package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = BlackSouls.MODID)
public class DamageTextRenderer {

    private static final int MAX_TEXTS = 160;
    private static final int NORMAL_TEXT_RGB = 0xF4E7D0;
    private static final int NORMAL_OUTLINE_RGB = 0x241315;
    private static final int CRIT_TEXT_RGB = 0xFF554D;
    private static final int CRIT_FLASH_RGB = 0xFFE09A;
    private static final int CRIT_OUTLINE_RGB = 0x3B0708;
    private static final int CRIT_GLOW_RGB = 0xE99A36;

    public static class DamageText {
        public double x, y, z;
        public double prevX, prevY, prevZ;
        public double velocityX, velocityY, velocityZ;
        public final long damage;
        public final boolean isCrit;
        public int age;
        public final int maxAge;
        public final float phase;
        public final String text;

        public DamageText(double x, double y, double z, long damage, boolean isCrit) {
            double angle = Math.random() * Math.PI * 2.0D;
            double spawnRadius = 0.045D + Math.random() * 0.085D;
            this.x = x + Math.cos(angle) * spawnRadius;
            this.y = y + (Math.random() - 0.5D) * 0.08D;
            this.z = z + Math.sin(angle) * spawnRadius;
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            this.damage = damage;
            this.isCrit = isCrit;
            this.age = 0;
            this.maxAge = isCrit ? 40 : 32;
            double lateralSpeed = (isCrit ? 0.016D : 0.010D) * (0.7D + Math.random() * 0.6D);
            this.velocityX = Math.cos(angle) * lateralSpeed;
            this.velocityZ = Math.sin(angle) * lateralSpeed;
            this.velocityY = (isCrit ? 0.067D : 0.048D) + Math.random() * 0.012D;
            this.phase = (float) (Math.random() * Math.PI * 2.0D);
            this.text = Long.toString(damage) + (isCrit ? "!" : "");
        }
    }

    private static final Deque<DamageText> TEXTS = new ArrayDeque<>();

    public static void addText(double x, double y, double z, long damage, boolean isCrit) {
        while (TEXTS.size() >= MAX_TEXTS) {
            TEXTS.removeFirst();
        }
        TEXTS.addLast(new DamageText(x, y, z, damage, isCrit));
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            TEXTS.clear();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                TEXTS.clear();
                return;
            }
            if (mc.isPaused()) return;
            Iterator<DamageText> it = TEXTS.iterator();
            while (it.hasNext()) {
                DamageText t = it.next();
                t.prevX = t.x;
                t.prevY = t.y;
                t.prevZ = t.z;
                t.age++;
                t.x += t.velocityX;
                t.y += t.velocityY + 0.004D;
                t.z += t.velocityZ;
                t.velocityX *= 0.90D;
                t.velocityY *= 0.88D;
                t.velocityZ *= 0.90D;
                if (t.age >= t.maxAge) it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (TEXTS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 view = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        var bufferSource = mc.renderBuffers().bufferSource();

        for (DamageText t : TEXTS) {
            poseStack.pushPose();

            float partialTick = event.getPartialTick();
            float renderAge = t.age + partialTick;
            float life = clamp01(renderAge / t.maxAge);
            double interpX = lerp(t.prevX, t.x, partialTick);
            double interpY = lerp(t.prevY, t.y, partialTick);
            double interpZ = lerp(t.prevZ, t.z, partialTick);

            float driftFade = 1.0F - smoothstep(0.45F, 1.0F, life);
            interpX += Math.sin(renderAge * 0.31F + t.phase) * 0.012D * driftFade;

            float impact = t.isCrit ? 1.0F - smoothstep(0.0F, 7.0F, renderAge) : 0.0F;
            if (impact > 0.0F) {
                interpX += Math.sin(renderAge * 4.7F + t.phase) * 0.017D * impact;
                interpY += Math.cos(renderAge * 5.3F + t.phase) * 0.011D * impact;
            }

            poseStack.translate(interpX - view.x, interpY - view.y, interpZ - view.z);
            poseStack.mulPose(camera.rotation());

            if (t.isCrit) {
                float rotation = (float) Math.sin(renderAge * 3.4F + t.phase) * 3.2F * impact;
                poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
            }

            float baseScale = t.isCrit ? 0.044F : 0.032F;
            float popScale = entranceScale(t.isCrit, renderAge);
            float fadeStart = t.isCrit ? 0.70F : 0.64F;
            float fade = 1.0F - smoothstep(fadeStart, 1.0F, life);
            float alpha = clamp01(fade * smoothstep(0.0F, 1.5F, renderAge));
            float exitScale = 1.0F - 0.06F * smoothstep(fadeStart, 1.0F, life);
            float scale = baseScale * popScale * exitScale;

            // Font#adjustColor treats alpha values in the 0..3 range as fully opaque.
            // Stop submitting the text before the fade reaches that range so the
            // number cannot flash back for one frame at the end of its lifetime.
            if (alpha <= 0.02F) {
                poseStack.popPose();
                continue;
            }

            if (t.isCrit && impact > 0.0F) {
                drawTextLayer(mc, poseStack, bufferSource, t.text,
                        scale * (1.13F + impact * 0.04F),
                        withAlpha(CRIT_GLOW_RGB, alpha * impact * 0.58F));
            }

            int outlineRgb = t.isCrit ? CRIT_OUTLINE_RGB : NORMAL_OUTLINE_RGB;
            drawTextLayer(mc, poseStack, bufferSource, t.text,
                    scale * 1.075F, withAlpha(outlineRgb, alpha * 0.92F));

            int mainRgb = t.isCrit
                    ? lerpRgb(CRIT_FLASH_RGB, CRIT_TEXT_RGB,
                    smoothstep(0.0F, 7.0F, renderAge))
                    : NORMAL_TEXT_RGB;
            drawTextLayer(mc, poseStack, bufferSource, t.text,
                    scale, withAlpha(mainRgb, alpha));

            poseStack.popPose();
        }
        bufferSource.endBatch();
    }

    private static void drawTextLayer(Minecraft mc, PoseStack poseStack,
                                      MultiBufferSource bufferSource, String text,
                                      float scale, int color) {
        // Minecraft's font renderer promotes near-zero alpha colors to opaque.
        // Never submit those colors, including short-lived glow/outline layers.
        if ((color >>> 24) <= 3 || scale <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(-scale, -scale, scale);
        int textWidth = mc.font.width(text);
        mc.font.drawInBatch(
                text,
                -textWidth / 2.0F,
                -mc.font.lineHeight / 2.0F,
                color,
                false,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0,
                15728880
        );
        poseStack.popPose();
    }

    private static float entranceScale(boolean crit, float age) {
        float peakAge = crit ? 2.3F : 2.0F;
        float settleAge = crit ? 8.0F : 6.0F;
        float startScale = crit ? 0.58F : 0.68F;
        float peakScale = crit ? 1.48F : 1.22F;
        if (age < peakAge) {
            return lerp(startScale, peakScale, smoothstep(0.0F, peakAge, age));
        }
        if (age < settleAge) {
            return lerp(peakScale, 1.0F, smoothstep(peakAge, settleAge, age));
        }
        return 1.0F;
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(clamp01(alpha) * 255.0F)));
        return a << 24 | rgb & 0x00FFFFFF;
    }

    private static int lerpRgb(int from, int to, float progress) {
        float t = clamp01(progress);
        int r = Math.round(lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, t));
        int g = Math.round(lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, t));
        int b = Math.round(lerp(from & 0xFF, to & 0xFF, t));
        return r << 16 | g << 8 | b;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        if (edge0 == edge1) {
            return value < edge0 ? 0.0F : 1.0F;
        }
        float t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }
}
