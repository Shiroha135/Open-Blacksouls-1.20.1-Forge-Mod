package com.BlackSouls.BlackSoulsMod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.client.SceneSpawnerRenderer", remap = false)
public abstract class SceneSpawnerRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void blacksouls$renderWithWorldLighting(
            @Coerce Object blockEntityObject,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo callback
    ) {
        if (!(blockEntityObject instanceof BlockEntity blockEntity)
                || !(blockEntityObject instanceof SceneSpawnerBlockEntityAccessor accessor)) {
            return;
        }
        Level level = blockEntity.getLevel();
        BlockState camouflage = accessor.blacksouls$getCamouflageState();
        if (level == null || camouflage.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(camouflage);
        ModelData modelData = ModelData.EMPTY;
        long seed = camouflage.getSeed(pos);
        RandomSource random = RandomSource.create(seed);
        for (RenderType renderType : model.getRenderTypes(camouflage, random, modelData)) {
            random.setSeed(seed);
            VertexConsumer consumer = bufferSource.getBuffer(
                    RenderTypeHelper.getEntityRenderType(renderType, false)
            );
            dispatcher.renderBatched(
                    camouflage,
                    pos,
                    level,
                    poseStack,
                    consumer,
                    true,
                    random,
                    modelData,
                    renderType
            );
        }
        callback.cancel();
    }
}
