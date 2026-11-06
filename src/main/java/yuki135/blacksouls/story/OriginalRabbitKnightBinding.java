package yuki135.blacksouls.story;

import yuki135.blacksouls.compat.scene.OriginalNpcPlacementData;
import yuki135.blacksouls.compat.scene.OriginalNpcPlacements;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.entity.EntityRabbitKnight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OriginalRabbitKnightBinding {
    public static final String SOURCE_ID = "map053_ev011";
    public static final Vec3 POSITION = new Vec3(39.5D, -58.0D, 217.5D);
    private static final AABB BOUNDS = new AABB(38.0D, -59.0D, 216.0D, 41.0D, -55.0D, 219.0D);
    private static final Set<ChunkPos> CHUNKS = Set.of(new ChunkPos(2, 13), new ChunkPos(3, 13));
    private static final List<OriginalNpcPlacements.Landmark> LANDMARKS = List.of(
            new OriginalNpcPlacements.Landmark(new BlockPos(39, -59, 217), "minecraft:grass_block", Map.of()),
            new OriginalNpcPlacements.Landmark(new BlockPos(41, -58, 216), "minecraft:campfire", Map.of("facing", "south")),
            new OriginalNpcPlacements.Landmark(new BlockPos(41, -59, 218), "minecraft:stone_brick_stairs", Map.of("facing", "north", "half", "bottom")),
            new OriginalNpcPlacements.Landmark(new BlockPos(39, -60, 217), "blacksouls2:hei_mu", Map.of("facing", "north")));

    private OriginalRabbitKnightBinding() {
    }

    public static Set<ChunkPos> requiredChunks() {
        return CHUNKS;
    }

    public static boolean chunksReady(ServerLevel level) {
        return CHUNKS.stream().allMatch(chunk -> level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null
                && level.areEntitiesLoaded(chunk.toLong()));
    }

    public static EntityRabbitKnight resolve(ServerLevel level) {
        if (!OriginalNpcPlacements.hasDeployedMap(level) || !chunksReady(level)
                || LANDMARKS.stream().anyMatch(landmark -> !landmark.matches(level))) {
            throw new UnsupportedOperationException("rabbit_source_terrain");
        }
        List<EntityRabbitKnight> actors = level.getEntitiesOfClass(EntityRabbitKnight.class, BOUNDS,
                actor -> actor.isAlive() && !actor.isRemoved() && actor.getProfileId() == 217
                        && actor.position().distanceToSqr(POSITION) < .0625D);
        if (actors.size() != 1 || !level.getEntitiesOfClass(EntityOriginalNpc.class, BOUNDS,
                actor -> !actor.isRemoved() && SOURCE_ID.equals(actor.getNpcId())).isEmpty()) {
            throw new UnsupportedOperationException("rabbit_source_ambiguous");
        }
        EntityRabbitKnight actor = actors.get(0);
        var recorded = OriginalNpcPlacementData.get(level).entityId(SOURCE_ID);
        if (recorded != null && !recorded.equals(actor.getUUID())) {
            throw new UnsupportedOperationException("rabbit_source_identity");
        }
        return actor;
    }

    public static boolean matches(Entity entity) {
        if (!(entity instanceof EntityRabbitKnight) || !(entity.level() instanceof ServerLevel level)) return false;
        try {
            return resolve(level) == entity;
        } catch (UnsupportedOperationException ignored) {
            return false;
        }
    }

    public static boolean claim(EntityRabbitKnight actor) {
        if (!matches(actor)) return false;
        var data = OriginalNpcPlacementData.get((ServerLevel) actor.level());
        return actor.getUUID().equals(data.entityId(SOURCE_ID)) || data.record(SOURCE_ID, actor.getUUID());
    }
}
