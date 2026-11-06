package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.FishingRodSourceSprite;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.story.OriginalFishingRodEntry;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalFishingRodVisuals {
    private static volatile Level originalLevel;

    private OriginalFishingRodVisuals() {
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var block = OriginalFishingRodEntry.NATIVE_BLOCK;
        var origin = OriginalFishingRodEntry.DISPLAY_ORIGIN;
        AABB display = new AABB(origin, origin).inflate(4.25D);
        if (level == null || level != originalLevel || !HokoniwaDestination.DIMENSION.equals(level.dimension())
                || !level.hasChunkAt(block) || !level.getBlockState(block).is(Blocks.BARRIER)
                || !event.getFrustum().isVisible(display)) return;
        var actors = level.getEntitiesOfClass(EntityOriginalNpc.class, new AABB(block).inflate(3),
                actor -> OriginalFishingRodEntry.handles(actor.getNpcId()) && actor.isAlive() && !actor.isRemoved()
                        && actor.position().distanceToSqr(OriginalFishingRodEntry.CONTROLLER_POSITION) < 1.0E-8D);
        if (actors.size() != 1) return;
        var actor = actors.get(0);
        var texture = FishingRodSourceSprite.texture(actor);
        if (texture == null) return;
        var camera = event.getCamera().getPosition();
        var poses = event.getPoseStack();
        var output = minecraft.renderBuffers().bufferSource();
        poses.pushPose();
        try {
            poses.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
            FishingRodSourceSprite.render(actor, minecraft.getEntityRenderDispatcher().cameraOrientation(), poses,
                    output, LevelRenderer.getLightColor(level, block));
            output.endBatch(RenderType.entityTranslucent(texture));
        } finally {
            poses.popPose();
        }
    }
}
