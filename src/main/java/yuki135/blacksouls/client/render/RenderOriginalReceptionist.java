package yuki135.blacksouls.client.render;

import yuki135.blacksouls.entity.EntityOriginalReceptionist;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;

public final class RenderOriginalReceptionist extends EntityRenderer<EntityOriginalReceptionist> {
    private final BlockRenderDispatcher blocks;

    public RenderOriginalReceptionist(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.0F;
        shadowStrength = 0.0F;
    }

    @Override
    public void render(EntityOriginalReceptionist entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int opacity = Math.max(0, Math.min(255, entity.getSpriteOpacity()));
        if (!entity.isOriginalVisualBound() || entity.isInvisible() || opacity == 0) return;
        renderBlockModel(blocks, entity.visualBlockState(), entity.blockPosition(), entity.level(), poseStack, buffer, opacity);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void renderBlockModel(BlockRenderDispatcher blocks, BlockState state, BlockPos origin,
                                         BlockAndTintGetter world, PoseStack poseStack, MultiBufferSource buffer, int opacity) {
        if (opacity <= 0 || state.getRenderShape() != RenderShape.MODEL) return;
        var model = blocks.getBlockModel(state);
        long seed = state.getSeed(origin);
        poseStack.pushPose();
        try {
            poseStack.translate(-0.5D, 0.0D, -0.5D);
            for (RenderType source : model.getRenderTypes(state, RandomSource.create(seed), ModelData.EMPTY)) {
                RenderType target = opacity == 255 ? RenderTypeHelper.getMovingBlockRenderType(source)
                        : RenderType.translucentMovingBlock();
                VertexConsumer consumer = buffer.getBuffer(target);
                if (opacity != 255) consumer = new OpacityConsumer(consumer, opacity);
                blocks.renderBatched(state, origin, world, poseStack, consumer,
                        false, RandomSource.create(seed), ModelData.EMPTY, source);
            }
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityOriginalReceptionist entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private static final class OpacityConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int opacity;

        private OpacityConsumer(VertexConsumer delegate, int opacity) {
            this.delegate = delegate;
            this.opacity = opacity;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(red, green, blue, Math.max(0, Math.min(255, alpha)) * opacity / 255);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(red, green, blue, Math.max(0, Math.min(255, alpha)) * opacity / 255);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }

        @Override
        public VertexConsumer misc(VertexFormatElement element, int... rawData) {
            delegate.misc(element, rawData);
            return this;
        }
    }
}
