package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.WhiteBearSourceSprite;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalWhiteBearPlacement;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalWhiteBear;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalWhiteBearVisuals {
    private static volatile Level originalLevel;

    private OriginalWhiteBearVisuals() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level && level.isClientSide) {
            originalLevel = HokoniwaDestination.DIMENSION.equals(level.dimension()) ? level : null;
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() == originalLevel) originalLevel = null;
    }

    @SubscribeEvent
    public static void onWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                && event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        BlockPos block = OriginalWhiteBearPlacement.NATIVE_BLOCK_POSITION;
        if (level == null || level != originalLevel || !HokoniwaDestination.DIMENSION.equals(level.dimension()) || !level.hasChunkAt(block)) return;
        BlockState state = level.getBlockState(block);
        if (!OriginalWhiteBearPlacement.isNativeBlock(state)
                || !(minecraft.getBlockRenderer().getBlockModel(state) instanceof NativeModel)
                || !event.getFrustum().isVisible(new AABB(block).inflate(1))) return;
        var actors = level.getEntitiesOfClass(EntityOriginalWhiteBear.class, new AABB(block).inflate(1),
                actor -> actor.isNativeBlockBound() && !actor.isRemoved() && actor.isAlive()
                        && actor.position().distanceToSqr(OriginalWhiteBearPlacement.POSITION) < 1.0E-8D);
        EntityOriginalWhiteBear actor = actors.size() == 1 ? actors.get(0) : null;
        boolean nativeModel = actor == null || WhiteBearSourceSprite.usesNativeModel(actor);
        if (event.getStage() != (nativeModel ? RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                : RenderLevelStageEvent.Stage.AFTER_PARTICLES)) return;
        var camera = event.getCamera().getPosition();
        var poses = event.getPoseStack();
        var output = minecraft.renderBuffers().bufferSource();
        int light = LevelRenderer.getLightColor(level, block);
        poses.pushPose();
        try {
            if (nativeModel) {
                poses.translate(block.getX() - camera.x, block.getY() - camera.y, block.getZ() - camera.z);
                var layer = RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
                minecraft.getBlockRenderer().renderSingleBlock(state, poses, output, light,
                        OverlayTexture.NO_OVERLAY, ModelData.EMPTY, layer);
                output.endBatch(layer);
            } else {
                poses.translate(actor.getX() - camera.x, actor.getY() - camera.y, actor.getZ() - camera.z);
                WhiteBearSourceSprite.render(actor, minecraft.getEntityRenderDispatcher().cameraOrientation(), poses, output, light);
                var texture = WhiteBearSourceSprite.texture(actor);
                if (texture != null) output.endBatch(RenderType.entityTranslucent(texture));
            }
        } finally {
            poses.popPose();
        }
    }

    public static final class NativeModel extends BakedModelWrapper<BakedModel> {
        private static final ModelProperty<Boolean> DYNAMIC = new ModelProperty<>();

        public NativeModel(BakedModel original) {
            super(original);
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos position, BlockState state, ModelData data) {
            return super.getModelData(level, position, state, data).derive().with(DYNAMIC,
                    originalLevel != null && OriginalWhiteBearPlacement.NATIVE_BLOCK_POSITION.equals(position)
                            && OriginalWhiteBearPlacement.isNativeBlock(state)).build();
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType layer) {
            return Boolean.TRUE.equals(data.get(DYNAMIC)) ? List.of() : super.getQuads(state, side, random, data, layer);
        }
    }

    @Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Models {
        @SubscribeEvent
        public static void onModels(ModelEvent.ModifyBakingResult event) {
            var location = new ModelResourceLocation(new net.minecraft.resources.ResourceLocation("blacksouls2:bai_xiong_mao_shop"), "facing=north");
            event.getModels().computeIfPresent(location, (key, model) -> model instanceof NativeModel ? model : new NativeModel(model));
        }
    }
}
