package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBounds;
import com.BlackSouls.BlackSoulsMod.mixin.client.SceneSpawnerBlockEntityAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
@SuppressWarnings("removal")
public final class SceneSpawnerDebugRenderer {
    private static final ResourceLocation SCENE_SPAWNER =
            new ResourceLocation("blacksouls2", "scene_spawner");
    private static final int CHUNK_RADIUS = 4;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.player.getMainHandItem().is(BlackSouls.DEV_STAT_TOOL.get())) {
            return;
        }

        ClientLevel level = minecraft.level;
        int centerChunkX = SectionPos.blockToSectionCoord(minecraft.player.getBlockX());
        int centerChunkZ = SectionPos.blockToSectionCoord(minecraft.player.getBlockZ());
        PoseStack poseStack = event.getPoseStack();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        for (int chunkX = centerChunkX - CHUNK_RADIUS; chunkX <= centerChunkX + CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = centerChunkZ - CHUNK_RADIUS; chunkZ <= centerChunkZ + CHUNK_RADIUS; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!SCENE_SPAWNER.equals(ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock()))
                            || !(blockEntity instanceof SceneSpawnerBounds bounds)
                            || !(blockEntity instanceof SceneSpawnerBlockEntityAccessor accessor)
                            || blockEntity.getBlockPos().distToCenterSqr(minecraft.player.position()) > 16384.0D) {
                        continue;
                    }
                    renderSpawner(poseStack, consumer, blockEntity, bounds, accessor);
                }
            }
        }
        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void renderSpawner(
            PoseStack poseStack,
            VertexConsumer consumer,
            BlockEntity blockEntity,
            SceneSpawnerBounds bounds,
            SceneSpawnerBlockEntityAccessor accessor
    ) {
        double centerX = blockEntity.getBlockPos().getX() + 0.5D;
        double centerY = blockEntity.getBlockPos().getY() + 1.03D;
        double centerZ = blockEntity.getBlockPos().getZ() + 0.5D;
        double halfX = bounds.blacksouls$getRangeX() * 0.5D;
        double halfZ = bounds.blacksouls$getRangeZ() * 0.5D;
        AABB area = new AABB(
                centerX - halfX,
                centerY,
                centerZ - halfZ,
                centerX + halfX,
                centerY + 0.025D,
                centerZ + halfZ
        );
        LevelRenderer.renderLineBox(poseStack, consumer, area, 0.12F, 0.92F, 1.0F, 0.95F);

        double yaw = Math.toRadians(accessor.blacksouls$getYaw());
        double directionX = -Math.sin(yaw);
        double directionZ = Math.cos(yaw);
        double edgeX = Math.abs(directionX) < 1.0E-5D ? Double.POSITIVE_INFINITY : halfX / Math.abs(directionX);
        double edgeZ = Math.abs(directionZ) < 1.0E-5D ? Double.POSITIVE_INFINITY : halfZ / Math.abs(directionZ);
        double length = Math.max(0.2D, Math.min(edgeX, edgeZ) * 0.82D);
        double endX = centerX + directionX * length;
        double endZ = centerZ + directionZ * length;
        drawLine(poseStack, consumer, centerX, centerY + 0.035D, centerZ,
                endX, centerY + 0.035D, endZ, 1.0F, 0.38F, 0.08F, 1.0F);

        double headLength = Math.min(0.32D, length * 0.35D);
        double leftX = endX - directionX * headLength - directionZ * headLength * 0.65D;
        double leftZ = endZ - directionZ * headLength + directionX * headLength * 0.65D;
        double rightX = endX - directionX * headLength + directionZ * headLength * 0.65D;
        double rightZ = endZ - directionZ * headLength - directionX * headLength * 0.65D;
        drawLine(poseStack, consumer, endX, centerY + 0.035D, endZ,
                leftX, centerY + 0.035D, leftZ, 1.0F, 0.38F, 0.08F, 1.0F);
        drawLine(poseStack, consumer, endX, centerY + 0.035D, endZ,
                rightX, centerY + 0.035D, rightZ, 1.0F, 0.38F, 0.08F, 1.0F);
    }

    private static void drawLine(
            PoseStack poseStack,
            VertexConsumer consumer,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        consumer.vertex(matrix, (float) x1, (float) y1, (float) z1)
                .color(red, green, blue, alpha).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(matrix, (float) x2, (float) y2, (float) z2)
                .color(red, green, blue, alpha).normal(normal, nx, ny, nz).endVertex();
    }

    private SceneSpawnerDebugRenderer() {
    }
}
