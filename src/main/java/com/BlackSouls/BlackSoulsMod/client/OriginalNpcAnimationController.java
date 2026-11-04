package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.AnimationRegistry;
import com.BlackSouls.BlackSoulsMod.client.render.VFXAnimation;
import com.BlackSouls.BlackSoulsMod.client.render.VFXCell;
import com.BlackSouls.BlackSoulsMod.client.render.VFXFrame;
import com.BlackSouls.BlackSoulsMod.client.render.VFXSoundTiming;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcAnimationPacket;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalNpcAnimationController {
    private static final int MAX_ACTIVE = 128;
    private static final int MAX_HISTORY = 512;
    private static final int CELL_SIZE = 192;
    private static final float[] CORNER_X = {-0.5F, 0.5F, 0.5F, -0.5F};
    private static final float[] CORNER_Y = {-0.5F, -0.5F, 0.5F, 0.5F};
    private static final Map<Integer, Active> ACTIVE = new LinkedHashMap<>();
    private static final PlaybackHistory HISTORY = new PlaybackHistory(MAX_HISTORY);
    private static boolean historyLimitReported;
    private static ResourceLocation dimension;
    private static long pausedAt = -1L;

    private OriginalNpcAnimationController() {
    }

    public static void apply(ClientboundOriginalNpcAnimationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(packet.dimension())) return;
        if (dimension != null && !dimension.equals(packet.dimension())) clear();
        dimension = packet.dimension();
        Playback previous = HISTORY.get(packet.playbackId());
        VFXAnimation animation = previous == null ? AnimationRegistry.ANIMATIONS.get(packet.animationId()) : previous.animation;
        if (animation == null || animation.frames.isEmpty()) {
            BlackSouls.LOGGER.warn("Original character animation {} has no client frames", packet.animationId());
            return;
        }
        long now = animationTime();
        Reception received = HISTORY.receive(packet.playbackId(), packet.animationId(), animation,
                packet.elapsedFrames(), now, packet.newPlayback(), packet.paused());
        if (received.ended()) {
            ACTIVE.values().removeIf(active -> active.playbackId.equals(packet.playbackId()));
            historyLimitReported = false;
            return;
        }
        if (received.playback() == null) {
            if (!historyLimitReported) {
                BlackSouls.LOGGER.warn("Original NPC animation history limit reached; preserving pending playback cursors");
                historyLimitReported = true;
            }
            return;
        }
        if (!received.changed()) {
            update(now);
            return;
        }
        ACTIVE.values().removeIf(active -> active.playbackId.equals(packet.playbackId()));
        Active active = new Active(packet.entityId(), received.playback(), packet.position(),
                new Vec3(packet.x(), packet.y(), packet.z()));
        active.bind(minecraft.level.getEntity(packet.entityId()));
        if (!ACTIVE.containsKey(packet.entityId()) && ACTIVE.size() >= MAX_ACTIVE) {
            ACTIVE.remove(ACTIVE.keySet().iterator().next());
        }
        ACTIVE.put(packet.entityId(), active);
        update(now);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) update(animationTime());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) clear();
    }

    @SubscribeEvent
    public static void onWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        long now = animationTime();
        update(now);
        if (ACTIVE.isEmpty()) return;
        RenderState saved = new RenderState();
        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poses.pushPose();
        try {
            poses.translate(-camera.x, -camera.y, -camera.z);
            prepareRender();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            for (Active active : ACTIVE.values()) {
                int frameIndex = cellFrameIndex(active.animation.frames.size(), active.timeline.elapsed(now));
                if (active.position == 3 || frameIndex < 0) continue;
                Entity target = Minecraft.getInstance().level.getEntity(active.entityId);
                Vec3 anchor = worldAnchor(active.origin,
                        target == null ? null : target.getPosition(event.getPartialTick()), active.position, active.height);
                poses.pushPose();
                try {
                    poses.translate(anchor.x, anchor.y, anchor.z);
                    poses.mulPose(event.getCamera().rotation());
                    renderWorldFrame(active.animation, active.animation.frames.get(frameIndex), poses.last().pose());
                } finally {
                    poses.popPose();
                }
            }
        } finally {
            poses.popPose();
            saved.restore();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onGuiRender(RenderGuiEvent.Pre event) {
        long now = animationTime();
        update(now);
        if (ACTIVE.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float canvasScale = screenScale(width, height);
        graphics.flush();
        RenderState saved = new RenderState();
        try {
            prepareRender();
            for (Active active : ACTIVE.values()) {
                int frameIndex = cellFrameIndex(active.animation.frames.size(), active.timeline.elapsed(now));
                if (active.position != 3 || frameIndex < 0) continue;
                renderScreenFrame(graphics, active.animation, active.animation.frames.get(frameIndex),
                        width / 2, height / 2, canvasScale);
            }
            graphics.flush();
        } finally {
            saved.restore();
        }
    }

    private static void update(long now) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || dimension != null && !dimension.equals(minecraft.level.dimension().location())) {
            clear();
            return;
        }
        Iterator<Active> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            Active active = iterator.next();
            Entity target = minecraft.level.getEntity(active.entityId);
            if (active.targetId != null && (target == null || target.isRemoved()
                    || !active.targetId.equals(target.getUUID()))) {
                iterator.remove();
                continue;
            }
            active.bind(target);
            long elapsed = active.timeline.elapsed(now);
            if (elapsed >= endFrame(active.animation.frames.size())) {
                iterator.remove();
                continue;
            }
            for (VFXSoundTiming sound : active.timeline.pollSounds(elapsed, active.animation.soundTimings)) {
                SoundEvent registered = ForgeRegistries.SOUND_EVENTS.getValue(sound.sound());
                if (registered == null) continue;
                SimpleSoundInstance instance = SimpleSoundInstance.forUI(registered, sound.pitch(), sound.volume());
                if (TurnBattleAudioGate.isActive()) TurnBattleAudioGate.play(instance);
                else minecraft.getSoundManager().play(instance);
            }
        }
    }

    private static void renderWorldFrame(VFXAnimation animation, VFXFrame frame, Matrix4f matrix) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        for (VFXCell cell : frame.cells) {
            boolean second = cell.textureIndex >= 100;
            int pattern = second ? cell.textureIndex - 100 : cell.textureIndex;
            ResourceLocation texture = second ? animation.texture2 : animation.texture1;
            int rows = second ? animation.rows2 : animation.rows1;
            if (texture == null || rows <= 0 || animation.cols <= 0 || pattern < 0) continue;
            int column = pattern % animation.cols;
            int row = pattern / animation.cols;
            float u0 = column / (float) animation.cols;
            float u1 = (column + 1) / (float) animation.cols;
            float v0 = row / (float) rows;
            float v1 = (row + 1) / (float) rows;
            float angle = (float) Math.toRadians(-cell.rotation);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            RenderSystem.setShaderTexture(0, texture);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int index = 0; index < 4; index++) {
                float x = CORNER_X[index] * (cell.mirror ? -1.0F : 1.0F) * cell.scaleX * 3.5F;
                float y = CORNER_Y[index] * cell.scaleY * 3.5F;
                float rotatedX = x * cos - y * sin + cell.offsetX * 0.4F;
                float rotatedY = x * sin + y * cos + cell.offsetY * 0.4F;
                buffer.vertex(matrix, -rotatedX, -rotatedY, 0.0F)
                        .uv(index == 0 || index == 3 ? u0 : u1, index < 2 ? v0 : v1)
                        .color(1.0F, 1.0F, 1.0F, cell.alpha).endVertex();
            }
            tesselator.end();
        }
    }

    private static void renderScreenFrame(GuiGraphics graphics, VFXAnimation animation, VFXFrame frame,
                                           int centerX, int centerY, float scale) {
        for (VFXCell cell : frame.cells) {
            boolean second = cell.textureIndex >= 100;
            int pattern = second ? cell.textureIndex - 100 : cell.textureIndex;
            ResourceLocation texture = second ? animation.texture2 : animation.texture1;
            int rows = second ? animation.rows2 : animation.rows1;
            if (texture == null || rows <= 0 || animation.cols <= 0 || pattern < 0) continue;
            int width = Math.max(1, Math.round(CELL_SIZE * cell.scaleX * 2.0F * scale));
            int height = Math.max(1, Math.round(CELL_SIZE * cell.scaleY * 2.0F * scale));
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(centerX + cell.offsetX * 32.0F * scale,
                        centerY + cell.offsetY * 32.0F * scale, 220.0F);
                graphics.pose().mulPose(Axis.ZP.rotationDegrees(cell.rotation));
                if (cell.mirror) graphics.pose().scale(-1.0F, 1.0F, 1.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, cell.alpha);
                graphics.blit(texture, -width / 2, -height / 2, width, height,
                        pattern % animation.cols * CELL_SIZE, pattern / animation.cols * CELL_SIZE,
                        CELL_SIZE, CELL_SIZE, animation.cols * CELL_SIZE, rows * CELL_SIZE);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    private static void prepareRender() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static long animationTime() {
        long now = System.nanoTime();
        if (Minecraft.getInstance().isPaused()) {
            if (pausedAt < 0L) pausedAt = now;
            return pausedAt;
        }
        if (pausedAt >= 0L) {
            long duration = now - pausedAt;
            HISTORY.shift(duration);
            pausedAt = -1L;
        }
        return now;
    }

    public static void clear() {
        ACTIVE.clear();
        HISTORY.clear();
        historyLimitReported = false;
        dimension = null;
        pausedAt = -1L;
    }

    public static int cellFrameIndex(int frameCount, long elapsedSourceFrames) {
        if (frameCount <= 0 || elapsedSourceFrames < 1L || elapsedSourceFrames >= endFrame(frameCount)) return -1;
        return (int) ((elapsedSourceFrames - 1L) / 4L);
    }

    public static long endFrame(int frameCount) {
        return 4L * Math.max(0, frameCount) + 1L;
    }

    public static double anchorHeight(int position, double spriteHeight) {
        return position == 0 ? spriteHeight : position == 1 ? spriteHeight / 2.0D : 0.0D;
    }

    public static Vec3 worldAnchor(Vec3 fallback, Vec3 current, int position, double initialSpriteHeight) {
        return (current == null ? fallback : current).add(0.0D, anchorHeight(position, initialSpriteHeight), 0.0D);
    }

    public static float screenScale(int width, int height) {
        return Math.min(width / 640.0F, height / 480.0F);
    }

    public static final class Timeline {
        private long elapsedAtStart;
        private long startedAt;
        private int nextSound;
        private boolean serverPaused;

        public Timeline(long elapsed, long now, boolean newPlayback, List<VFXSoundTiming> sounds) {
            this(elapsed, now, newPlayback, false, sounds);
        }

        public Timeline(long elapsed, long now, boolean newPlayback, boolean paused, List<VFXSoundTiming> sounds) {
            elapsedAtStart = Math.max(0L, elapsed);
            startedAt = now;
            serverPaused = paused;
            if (!newPlayback || paused) skipThrough(elapsedAtStart, sounds);
        }

        public long elapsed(long now) {
            if (serverPaused) return elapsedAtStart;
            long nanos = Math.max(0L, now - startedAt);
            return elapsedAtStart + nanos / 1_000_000_000L * 60L
                    + nanos % 1_000_000_000L * 60L / 1_000_000_000L;
        }

        public void synchronize(long elapsed, long now, boolean paused, List<VFXSoundTiming> sounds) {
            elapsedAtStart = Math.max(0L, elapsed);
            startedAt = now;
            serverPaused = paused;
            skipThrough(elapsedAtStart, sounds);
        }

        public boolean paused() {
            return serverPaused;
        }

        public void shift(long pausedNanos) {
            startedAt += Math.max(0L, pausedNanos);
        }

        public List<VFXSoundTiming> pollSounds(long elapsed, List<VFXSoundTiming> sounds) {
            if (serverPaused) return List.of();
            int first = nextSound;
            skipThrough(elapsed, sounds);
            return List.copyOf(sounds.subList(first, nextSound));
        }

        private void skipThrough(long elapsed, List<VFXSoundTiming> sounds) {
            while (nextSound < sounds.size() && 1L + 4L * sounds.get(nextSound).frame() <= elapsed) nextSound++;
        }
    }

    public static final class PlaybackHistory {
        private final int capacity;
        private final Map<UUID, Playback> playbacks = new LinkedHashMap<>();

        public PlaybackHistory(int capacity) {
            if (capacity < 1) throw new IllegalArgumentException("Invalid animation history capacity");
            this.capacity = capacity;
        }

        public Reception receive(UUID id, int animationId, VFXAnimation animation, long elapsed,
                                 long now, boolean newPlayback, boolean paused) {
            Playback existing = playbacks.get(id);
            if (existing != null && existing.animationId != animationId) {
                throw new IllegalArgumentException("Animation identity changed within one playback");
            }
            if (elapsed >= endFrame(animation.frames.size())) {
                playbacks.remove(id);
                return new Reception(null, false, true);
            }
            if (existing != null) {
                if (newPlayback) return new Reception(existing, false, false);
                existing.timeline.synchronize(elapsed, now, paused, existing.animation.soundTimings);
                return new Reception(existing, true, false);
            }
            if (playbacks.size() >= capacity) return new Reception(null, false, false);
            Playback playback = new Playback(id, animationId, animation,
                    new Timeline(elapsed, now, newPlayback, paused, animation.soundTimings));
            playbacks.put(id, playback);
            return new Reception(playback, true, false);
        }

        public Playback get(UUID id) {
            return playbacks.get(id);
        }

        public int size() {
            return playbacks.size();
        }

        public void shift(long pausedNanos) {
            for (Playback playback : playbacks.values()) playback.timeline.shift(pausedNanos);
        }

        public void clear() {
            playbacks.clear();
        }
    }

    public record Reception(Playback playback, boolean changed, boolean ended) {
    }

    public static final class Playback {
        private final UUID id;
        private final int animationId;
        private final VFXAnimation animation;
        private final Timeline timeline;
        private UUID targetId;
        private double height = 1.9D;

        private Playback(UUID id, int animationId, VFXAnimation animation, Timeline timeline) {
            this.id = id;
            this.animationId = animationId;
            this.animation = animation;
            this.timeline = timeline;
        }

        public Timeline timeline() {
            return timeline;
        }
    }

    private static final class Active {
        private final int entityId;
        private final UUID playbackId;
        private final Playback playback;
        private final VFXAnimation animation;
        private final int position;
        private final Vec3 origin;
        private final Timeline timeline;
        private UUID targetId;
        private double height;

        private Active(int entityId, Playback playback, int position, Vec3 origin) {
            this.entityId = entityId;
            this.playbackId = playback.id;
            this.playback = playback;
            this.animation = playback.animation;
            this.position = position;
            this.origin = origin;
            this.timeline = playback.timeline;
            this.targetId = playback.targetId;
            this.height = playback.height;
        }

        private void bind(Entity target) {
            if (targetId == null && target != null && !target.isRemoved()) {
                targetId = target.getUUID();
                height = target instanceof EntityOriginalNpc ? 1.9D : target.getBbHeight();
                playback.targetId = targetId;
                playback.height = height;
            }
        }
    }

    private static final class RenderState {
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int texture = RenderSystem.getShaderTexture(0);
        private final float[] color = RenderSystem.getShaderColor().clone();
        private final ShaderInstance shader = RenderSystem.getShader();

        private void restore() {
            RenderSystem.depthMask(depthMask);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            if (shader != null) RenderSystem.setShader(() -> shader);
        }
    }
}
