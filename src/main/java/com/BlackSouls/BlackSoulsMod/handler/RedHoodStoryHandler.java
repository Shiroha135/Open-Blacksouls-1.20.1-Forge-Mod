package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import com.BlackSouls.BlackSoulsMod.entity.EntityRedHood;
import com.BlackSouls.BlackSoulsMod.entity.RedHoodDialogue;
import com.BlackSouls.BlackSoulsMod.util.BonfireMetadata;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.Optional;

public final class RedHoodStoryHandler {
    public static void initializePlacedEntity(EntityRedHood entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        BSWorldData data = BSWorldData.get(level.getServer().overworld());
        entity.setStoryContext(
                data.getRedHoodStoryStage(),
                RedHoodDialogue.resolveScene("", data.getRedHoodStoryStage()),
                findNearbyBonfire(level, entity.blockPosition()).orElse(null)
        );
    }

    public static void onBonfireRest(ServerPlayer player, ServerLevel level, BlockPos bonfirePos) {
        BSWorldData data = BSWorldData.get(level.getServer().overworld());
        GlobalPos bonfire = GlobalPos.of(level.dimension(), bonfirePos);
        if (!data.isRedHoodAwaitingNextBonfire() || data.isRedHoodLastBonfire(bonfire)) {
            return;
        }
        AABB nearby = new AABB(bonfirePos).inflate(12.0D);
        if (!level.getEntitiesOfClass(EntityRedHood.class, nearby, Entity::isAlive).isEmpty()) {
            return;
        }
        BlockPos spawnPos = findSpawnPosition(level, bonfirePos);
        EntityRedHood entity = BSEntityRegistry.RED_HOOD.get().create(level);
        if (entity == null) {
            return;
        }
        BonfireMetadata.Data metadata = BonfireMetadata.read(level, bonfirePos);
        entity.setStoryContext(
                data.getRedHoodStoryStage(),
                RedHoodDialogue.resolveScene(metadata.name(), data.getRedHoodStoryStage()),
                bonfire
        );
        entity.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                player.getYRot() + 180.0F,
                0.0F
        );
        if (!level.noCollision(entity)) {
            return;
        }
        level.addFreshEntity(entity);
        data.markRedHoodSpawned(bonfire);
    }

    public static void completeDialogue(ServerPlayer player, EntityRedHood entity, int stage) {
        ServerLevel level = player.serverLevel();
        BSWorldData data = BSWorldData.get(level.getServer().overworld());
        if (entity.level() != level
                || entity.getStoryStage() != stage
                || data.getRedHoodStoryStage() != stage
                || data.isRedHoodAwaitingNextBonfire()) {
            return;
        }
        if (!RedHoodDialogue.hasNext(stage)) {
            return;
        }
        GlobalPos anchor = entity.getAnchorBonfire()
                .or(() -> findNearbyBonfire(level, entity.blockPosition()))
                .orElse(null);
        data.advanceRedHoodStory(anchor);
        entity.discard();
    }

    public static void reset(ServerPlayer player, EntityRedHood entity) {
        BSWorldData data = BSWorldData.get(player.server.overworld());
        data.resetRedHoodStory();
        entity.setStoryContext(0, RedHoodDialogue.resolveScene("", 0), null);
    }

    private static Optional<GlobalPos> findNearbyBonfire(ServerLevel level, BlockPos center) {
        return BlockPos.betweenClosedStream(center.offset(-8, -4, -8), center.offset(8, 4, 8))
                .filter(pos -> level.getBlockState(pos).is(BlockTags.CAMPFIRES))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(center)))
                .map(pos -> GlobalPos.of(level.dimension(), pos.immutable()));
    }

    private static BlockPos findSpawnPosition(ServerLevel level, BlockPos bonfirePos) {
        int[][] offsets = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2}
        };
        for (int[] offset : offsets) {
            BlockPos candidate = bonfirePos.offset(offset[0], 0, offset[1]);
            BlockState floor = level.getBlockState(candidate.below());
            if (floor.isFaceSturdy(level, candidate.below(), Direction.UP)
                    && level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()
                    && level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()) {
                return candidate;
            }
        }
        return bonfirePos.above();
    }

    private RedHoodStoryHandler() {
    }
}
