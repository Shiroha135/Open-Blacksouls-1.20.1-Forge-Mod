package com.BlackSouls.BlackSoulsMod.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class FogGateEffectRenderer extends RenderType {
    private static final int SUBDIVISIONS = 8;
    private static final RenderType FOG_GATE = create(
            "blacksouls_fog_gate",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            512,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> ShaderHelper.fogGateShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false)
    );

    private FogGateEffectRenderer(
            String name,
            VertexFormat format,
            VertexFormat.Mode mode,
            int bufferSize,
            boolean affectsCrumbling,
            boolean sortOnUpload,
            Runnable setupState,
            Runnable clearState
    ) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void render(
            BlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        Level level = blockEntity.getLevel();
        BlockState state = blockEntity.getBlockState();
        if (level == null || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return;
        }
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        float rotation = switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
        BlockPos pos = blockEntity.getBlockPos();
        float horizontalBase = facing.getAxis() == Direction.Axis.Z ? pos.getX() : pos.getZ();
        float verticalBase = pos.getY();
        float time = (level.getGameTime() + partialTick) * 5.0F;
        boolean mirrorHorizontal = facing == Direction.EAST || facing == Direction.SOUTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        drawSurface(
                bufferSource.getBuffer(FOG_GATE),
                poseStack.last(),
                horizontalBase,
                verticalBase,
                time,
                mirrorHorizontal
        );
        poseStack.popPose();
    }

    private static void drawSurface(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float horizontalBase,
            float verticalBase,
            float time,
            boolean mirrorHorizontal
    ) {
        for (int y = 0; y < SUBDIVISIONS; y++) {
            float y0 = (float) y / SUBDIVISIONS;
            float y1 = (float) (y + 1) / SUBDIVISIONS;
            for (int x = 0; x < SUBDIVISIONS; x++) {
                float x0 = (float) x / SUBDIVISIONS;
                float x1 = (float) (x + 1) / SUBDIVISIONS;
                float h0 = horizontalBase + (mirrorHorizontal ? 1.0F - x0 : x0);
                float h1 = horizontalBase + (mirrorHorizontal ? 1.0F - x1 : x1);
                vertex(consumer, pose, x0, y0, surfaceDepth(h0, verticalBase + y0, time), h0, verticalBase + y0);
                vertex(consumer, pose, x1, y0, surfaceDepth(h1, verticalBase + y0, time), h1, verticalBase + y0);
                vertex(consumer, pose, x1, y1, surfaceDepth(h1, verticalBase + y1, time), h1, verticalBase + y1);
                vertex(consumer, pose, x0, y1, surfaceDepth(h0, verticalBase + y1, time), h0, verticalBase + y1);
            }
        }
    }

    private static float surfaceDepth(float worldHorizontal, float worldVertical, float time) {
        return (float) (
                Math.sin(worldHorizontal * 4.7F + worldVertical * 1.3F + time * 0.055F) * 0.010F
                        + Math.sin(worldHorizontal * 9.1F - worldVertical * 2.2F - time * 0.037F) * 0.005F
        );
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float worldHorizontal,
            float worldVertical
    ) {
        consumer.vertex(pose.pose(), x - 0.5F, y - 0.5F, z)
                .color(255, 255, 255, 255)
                .uv(worldHorizontal * 0.32F, worldVertical * 0.32F)
                .endVertex();
    }
}
