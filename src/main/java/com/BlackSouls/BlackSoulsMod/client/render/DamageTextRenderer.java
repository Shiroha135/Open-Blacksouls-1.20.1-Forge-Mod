package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = BlackSouls.MODID)
public class DamageTextRenderer {

    private static final int MAX_TEXTS = 160;

    public static class DamageText {
        public double x, y, z;
        public double motionY;
        public float damage;
        public boolean isCrit;
        public int age;
        public int maxAge;
        public double randomOffsetX;
        public String text;

        public DamageText(double x, double y, double z, float damage, boolean isCrit) {
            this.x = x; this.y = y; this.z = z;
            this.damage = damage; this.isCrit = isCrit;
            this.age = 0;
            this.maxAge = isCrit ? 45 : 30;
            this.motionY = isCrit ? 0.08 : 0.04;
            this.randomOffsetX = (Math.random() - 0.5) * 0.1;
            this.text = Integer.toString(Math.round(damage)) + (isCrit ? "!" : "");
        }
    }

    private static final List<DamageText> TEXTS = new ArrayList<>();

    public static void addText(double x, double y, double z, float damage, boolean isCrit) {
        while (TEXTS.size() >= MAX_TEXTS) {
            TEXTS.remove(0);
        }
        TEXTS.add(new DamageText(x, y, z, damage, isCrit));
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
            if (Minecraft.getInstance().level == null) {
                TEXTS.clear();
                return;
            }
            Iterator<DamageText> it = TEXTS.iterator();
            while (it.hasNext()) {
                DamageText t = it.next();
                t.age++;
                t.y += t.motionY;
                t.x += t.randomOffsetX;
                t.z += t.randomOffsetX;
                t.motionY -= 0.005;
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

            double interpX = (t.x - t.randomOffsetX) + t.randomOffsetX * event.getPartialTick() - view.x;
            double interpY = (t.y - t.motionY) + t.motionY * event.getPartialTick() - view.y;
            double interpZ = (t.z - t.randomOffsetX) + t.randomOffsetX * event.getPartialTick() - view.z;

            poseStack.translate(interpX, interpY, interpZ);
            poseStack.mulPose(camera.rotation());

            float scale = t.isCrit ? 0.045F : 0.03F;
            float ageScale = 1.0F;
            if (t.age > t.maxAge - 10) ageScale = (t.maxAge - t.age) / 10.0F;
            scale *= ageScale;

            poseStack.scale(-scale, -scale, scale);

            String text = t.text;

            int color = t.isCrit ? 0xFFAA0000 : 0xFFFFFFFF;
            int textWidth = mc.font.width(text);
            int alpha = (int)(ageScale * 255);
            int finalColor = (alpha << 24) | (color & 0x00FFFFFF);

            mc.font.drawInBatch(
                    text,
                    -textWidth / 2.0F,
                    0,
                    finalColor,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    15728880
            );

            poseStack.popPose();
        }
        bufferSource.endBatch();
    }
}
