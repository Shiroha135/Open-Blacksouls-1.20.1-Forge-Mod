package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = BlackSouls.MODID)
public class ClientVFXHandler {

    private static final int MAX_ACTIVE_ANIMATIONS = 128;
    private static final float[] CORNER_X = {-0.5F, 0.5F, 0.5F, -0.5F};
    private static final float[] CORNER_Y = {-0.5F, -0.5F, 0.5F, 0.5F};

    private static final Deque<ActiveAnim> activeAnimations = new ArrayDeque<>();
    private static long pauseStartedAt = -1L;

    public static void playAnim(int animId, double x, double y, double z) {
        VFXAnimation anim = AnimationRegistry.ANIMATIONS.get(animId);
        if (anim != null) {
            addActiveAnimation(new ActiveAnim(anim, x, y, z, getAnimationTime(), 66));
        }
    }

    public static void spawnVFX(String texturePath, double x, double y, double z, int cols, int rows, int[] frameSequence, float scale) {
        if (frameSequence == null || frameSequence.length == 0) return;
        VFXAnimation tempAnim = new VFXAnimation(texturePath, "", cols, rows);
        for (int frameIndex : frameSequence) {
            VFXFrame frame = new VFXFrame();
            frame.addCell(new VFXCell(frameIndex, 0f, 0f, scale, scale, 0f, 1.0f, false));
            tempAnim.frames.add(frame);
        }
        addActiveAnimation(new ActiveAnim(tempAnim, x, y, z, getAnimationTime(), 40));
    }

    private static void addActiveAnimation(ActiveAnim activeAnim) {
        while (activeAnimations.size() >= MAX_ACTIVE_ANIMATIONS) {
            activeAnimations.removeFirst();
        }
        activeAnimations.addLast(activeAnim);
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            activeAnimations.clear();
            pauseStartedAt = -1L;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            updatePauseState(Util.getMillis());
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        renderActiveAnimations(event.getCamera(), event.getPoseStack());
    }

    private static void renderActiveAnimations(Camera camera, PoseStack poseStack) {
        if (activeAnimations.isEmpty()) return;

        Vec3 view = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-view.x, -view.y, -view.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        long currentTime = getAnimationTime();
        Iterator<ActiveAnim> it = activeAnimations.iterator();

        while (it.hasNext()) {
            ActiveAnim active = it.next();

            int frameIndex = (int) ((currentTime - active.startTime) / active.frameDuration);
            if (frameIndex >= active.anim.frames.size()) {
                it.remove();
                continue;
            }

            VFXFrame frame = active.anim.frames.get(frameIndex);

            for (VFXCell cell : frame.cells) {
                boolean isTex2 = cell.textureIndex >= 100;
                int actualPattern = isTex2 ? cell.textureIndex - 100 : cell.textureIndex;
                ResourceLocation currentTex = isTex2 ? active.anim.texture2 : active.anim.texture1;
                int currentRows = isTex2 ? active.anim.rows2 : active.anim.rows1;

                if (currentTex == null) continue;

                int col = actualPattern % active.anim.cols;
                int row = actualPattern / active.anim.cols;
                float u0 = (float) col / active.anim.cols;
                float u1 = (float) (col + 1) / active.anim.cols;
                float v0 = (float) row / currentRows;
                float v1 = (float) (row + 1) / currentRows;

                poseStack.pushPose();
                poseStack.translate(active.x, active.y, active.z);
                poseStack.mulPose(camera.rotation());

                PoseStack.Pose last = poseStack.last();
                Matrix4f matrix = last.pose();

                float baseScale = 3.5F;
                float rad = (float) Math.toRadians(-cell.rotation);
                float cos = (float) Math.cos(rad);
                float sin = (float) Math.sin(rad);

                RenderSystem.setShaderTexture(0, currentTex);
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                for (int i = 0; i < 4; i++) {
                    float x = CORNER_X[i];
                    float y = CORNER_Y[i];
                    if (cell.mirror) x = -x;

                    x *= cell.scaleX * baseScale;
                    y *= cell.scaleY * baseScale;

                    float rx = x * cos - y * sin;
                    float ry = x * sin + y * cos;

                    float fx = rx + cell.offsetX * 0.4f;
                    float fy = ry + cell.offsetY * 0.4f;

                    buffer.vertex(matrix, -fx, -fy, 0.0F)
                            .uv(i == 0 || i == 3 ? u0 : u1, i < 2 ? v0 : v1)
                            .color(1.0F, 1.0F, 1.0F, cell.alpha)
                            .endVertex();
                }

                tesselator.end();
                poseStack.popPose();
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private static long getAnimationTime() {
        long now = Util.getMillis();
        updatePauseState(now);
        return pauseStartedAt >= 0L ? pauseStartedAt : now;
    }

    private static void updatePauseState(long now) {
        if (Minecraft.getInstance().isPaused()) {
            if (pauseStartedAt < 0L) {
                pauseStartedAt = now;
            }
            return;
        }

        if (pauseStartedAt >= 0L) {
            long pausedDuration = now - pauseStartedAt;
            for (ActiveAnim active : activeAnimations) {
                active.startTime += pausedDuration;
            }
            pauseStartedAt = -1L;
        }
    }

    private static class ActiveAnim {
        VFXAnimation anim;
        double x, y, z;
        long startTime;
        int frameDuration;

        ActiveAnim(VFXAnimation anim, double x, double y, double z, long time, int duration) {
            this.anim = anim; this.x = x; this.y = y; this.z = z;
            this.startTime = time; this.frameDuration = duration;
        }
    }


}
