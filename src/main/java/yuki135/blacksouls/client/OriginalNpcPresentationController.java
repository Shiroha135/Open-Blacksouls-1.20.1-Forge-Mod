package yuki135.blacksouls.client;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPresentationPacket;
import yuki135.blacksouls.util.OriginalNpcTextures;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalNpcPresentationController {
    private static final ResourceLocation BALLOON = ResourceLocation.fromNamespaceAndPath(
            BlackSouls.MODID, "textures/gui/original_npc/balloon.png");
    private static OriginalNpcPresentationState state = new OriginalNpcPresentationState();
    private static final Map<Integer, Balloon> balloons = new HashMap<>();
    private static ResourceLocation dimension;
    private static TextureTarget capture;
    private static ShaderInstance screenShader;

    private OriginalNpcPresentationController() {
    }

    public static int pictureBrightness() {
        return state.brightness();
    }

    public static double[] pictureFlash() {
        return state.flash();
    }

    public static void apply(ClientboundOriginalNpcPresentationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(packet.dimension())) {
            return;
        }
        dimension = packet.dimension();
        switch (packet.commandCode()) {
            case yuki135.blacksouls.story.OriginalNonExplicitScenes.FADE_COMMAND -> {
                state = new OriginalNpcPresentationState();
                OriginalNpcPictureController.clear();
                OriginalSceneOmissionController.begin();
            }
            case 221, 222, 223, 224, 225 -> state.apply(packet.commandCode(), packet.values());
            case 213 -> balloons.put(packet.entityId(), new Balloon(packet.values().get(0).intValue(), 76));
            case 241, 242, 243, 244 -> OriginalNpcMusicController.apply(packet.commandCode(), packet.sound(), packet.values());
            case 250 -> {
                if (!packet.sound().isEmpty()) {
                    SimpleSoundInstance sound = new SimpleSoundInstance(
                            java.util.Objects.requireNonNull(ResourceLocation.tryParse(packet.sound())), SoundSource.NEUTRAL,
                            packet.values().get(0).floatValue() / 100.0F, packet.values().get(1).floatValue() / 100.0F,
                            RandomSource.create(), false, 0, net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,
                            0.0D, 0.0D, 0.0D, true);
                    if (TurnBattleAudioGate.isActive()) {
                        TurnBattleAudioGate.play(sound);
                    } else {
                        minecraft.getSoundManager().play(sound);
                    }
                }
            }
            default -> throw new IllegalArgumentException("Unsupported original NPC presentation packet " + packet.commandCode());
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player != null && !minecraft.player.isAlive()
                || dimension != null && !dimension.equals(minecraft.level.dimension().location())) {
            clear();
            return;
        }
        if (!minecraft.isPaused()) {
            state.advanceFrames(3);
            balloons.replaceAll((id, balloon) -> new Balloon(balloon.id(), balloon.remaining() - 3));
            balloons.entrySet().removeIf(entry -> entry.getValue().remaining() <= 0);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        state = new OriginalNpcPresentationState();
        balloons.clear();
        dimension = null;
        OriginalNpcMusicController.reset();
        OriginalNpcAnimationController.clear();
        if (capture != null) {
            capture.destroyBuffers();
            capture = null;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!state.active() || screenShader == null || Minecraft.getInstance().level == null) {
            return;
        }
        event.getGuiGraphics().flush();
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        if (capture == null || capture.width != main.width || capture.height != main.height) {
            if (capture != null) {
                capture.destroyBuffers();
            }
            capture = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, capture.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, capture.width, capture.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(false);
        ShaderInstance previous = RenderSystem.getShader();
        boolean previousDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean previousCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean previousBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int previousTexture = RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> screenShader);
            screenShader.setSampler("Sampler0", capture.getColorTextureId());
            RenderSystem.setShaderTexture(0, capture.getColorTextureId());
            double[] tone = state.tone();
            double[] flash = state.flash();
            screenShader.safeGetUniform("Tone").set((float) (tone[0] / 255.0D), (float) (tone[1] / 255.0D),
                    (float) (tone[2] / 255.0D), (float) (tone[3] / 255.0D));
            screenShader.safeGetUniform("Flash").set((float) (flash[0] / 255.0D), (float) (flash[1] / 255.0D),
                    (float) (flash[2] / 255.0D), (float) (flash[3] / 255.0D));
            screenShader.safeGetUniform("Brightness").set(state.brightness() / 255.0F);
            screenShader.safeGetUniform("Shake").set((float) (state.shake() / 640.0D));
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(-1.0D, -1.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
            buffer.vertex(1.0D, -1.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
            buffer.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 1.0F).endVertex();
            buffer.vertex(-1.0D, 1.0D, 0.0D).uv(0.0F, 1.0F).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShader(() -> previous);
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.depthMask(previousDepthMask);
            if (previousDepth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (previousCull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (previousBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        }
    }

    @SubscribeEvent
    public static void onWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || balloons.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (Map.Entry<Integer, Balloon> entry : balloons.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            int frame = OriginalNpcPresentationState.balloonFrame(entry.getValue().remaining());
            float u0 = frame / 8.0F;
            float u1 = (frame + 1) / 8.0F;
            float v0 = (entry.getValue().id() - 1) / 10.0F;
            float v1 = entry.getValue().id() / 10.0F;
            float height = entity.getBbHeight();
            if (entity instanceof EntityOriginalNpc npc && npc.currentPage() != null) {
                JsonObject graphic = npc.currentPage().graphic();
                JsonObject sheet = OriginalNpcTextures.character(graphic.get("character_name").getAsString());
                if (sheet != null) {
                    height = 1.9F * 32.0F / sheet.get("frameHeight").getAsInt();
                }
            }
            Vec3 position = entity.getPosition(event.getPartialTick()).add(0.0D, entity.getBbHeight(), 0.0D)
                    .subtract(event.getCamera().getPosition());
            PoseStack poses = event.getPoseStack();
            poses.pushPose();
            poses.translate(position.x, position.y, position.z);
            poses.mulPose(event.getCamera().rotation());
            poses.mulPose(Axis.YP.rotationDegrees(180.0F));
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(BALLOON));
            int light = 0xF000F0;
            PoseStack.Pose pose = poses.last();
            vertex(consumer, pose.pose(), pose.normal(), -height / 2.0F, 0.0F, u0, v1, light);
            vertex(consumer, pose.pose(), pose.normal(), height / 2.0F, 0.0F, u1, v1, light);
            vertex(consumer, pose.pose(), pose.normal(), height / 2.0F, height, u1, v0, light);
            vertex(consumer, pose.pose(), pose.normal(), -height / 2.0F, height, u0, v0, light);
            poses.popPose();
        }
        buffers.endBatch(RenderType.entityTranslucent(BALLOON));
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float u, float v, int light) {
        consumer.vertex(matrix, x, y, 0.0F).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    private record Balloon(int id, int remaining) {
    }

    @Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID, "original_npc_screen"),
                    DefaultVertexFormat.POSITION_TEX), shader -> screenShader = shader);
        }
    }
}
