package yuki135.blacksouls.story;

import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.compat.scene.OriginalNpcPlacements;
import yuki135.blacksouls.compat.scene.SceneSpawnerBossData;
import yuki135.blacksouls.entity.EntityCheshireCat;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.util.BSOriginalNpcData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OriginalNpcCharacterBindings {
    public static final String CHESHIRE_RABBIT_HOLE = "map051_ev059";
    private static final Vec3 CHESHIRE_POSITION = new Vec3(79.5D, -25.0D, 170.5D);
    private static final AABB CHESHIRE_BOUNDS = new AABB(78.0D, -26.0D, 169.0D, 81.0D, -23.0D, 172.0D);
    private static final Set<ChunkPos> CHESHIRE_CHUNKS = Set.of(new ChunkPos(4, 10), new ChunkPos(5, 10));
    private static final List<OriginalNpcPlacements.Landmark> CHESHIRE_LANDMARKS = List.of(
            landmark(79, -29, 170, "minecraft:black_concrete", Map.of()),
            landmark(79, -29, 169, "blacksouls2:xie_kuai", Map.of()),
            landmark(79, -26, 170, "fantasyfurniture:nordic/shelf", Map.of("facing", "north", "connection", "right")),
            landmark(80, -26, 170, "fantasyfurniture:nordic/shelf", Map.of("facing", "north", "connection", "left")),
            landmark(79, -28, 171, "minecraft:polished_diorite", Map.of()));

    private OriginalNpcCharacterBindings() {
    }

    public static Entity resolveEvent(ServerPlayer player, EntityOriginalNpc subject, int eventId) {
        if (player == null || subject == null || player.level() != subject.level()
                || !subject.isAlive() || subject.isRemoved()) {
            throw new UnsupportedOperationException("event_character_context:" + eventId);
        }
        if (eventId < 0) return player;
        if (eventId == 0) return subject;
        var source = BSOriginalNpcData.getEvent(subject.getNpcId());
        if (source == null) throw new UnsupportedOperationException("event_character_source");
        if (eventId == source.eventId()) return subject;
        EntityOriginalNpc resolved = null;
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (!(entity instanceof EntityOriginalNpc candidate) || !candidate.isAlive() || candidate.isRemoved()) continue;
            var definition = BSOriginalNpcData.getEvent(candidate.getNpcId());
            if (definition == null || definition.mapId() != source.mapId() || definition.eventId() != eventId) continue;
            if (resolved != null) throw new UnsupportedOperationException("event_character_ambiguous:" + eventId);
            resolved = candidate;
        }
        if (resolved == null) throw new UnsupportedOperationException("event_character_unloaded:" + eventId);
        return resolved;
    }

    public static Set<ChunkPos> cheshireRequiredChunks() {
        return CHESHIRE_CHUNKS;
    }

    public static AABB cheshireSearchBounds() {
        return CHESHIRE_BOUNDS;
    }

    public static boolean cheshireChunksReady(ServerLevel level) {
        for (ChunkPos chunk : CHESHIRE_CHUNKS) {
            if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null || !level.areEntitiesLoaded(chunk.toLong())) {
                return false;
            }
        }
        return true;
    }

    public static boolean cheshireTerrainMatches(ServerLevel level) {
        return OriginalNpcPlacements.hasDeployedMap(level) && cheshireChunksReady(level)
                && CHESHIRE_LANDMARKS.stream().allMatch(landmark -> landmark.matches(level));
    }

    public static EntityCheshireCat resolve(ServerLevel level, String sourceId) {
        if (!CHESHIRE_RABBIT_HOLE.equals(sourceId)) {
            throw new UnsupportedOperationException("animation_character_unmapped:" + sourceId);
        }
        if (!cheshireTerrainMatches(level)) {
            throw new UnsupportedOperationException("animation_character_terrain:" + sourceId);
        }
        List<EntityCheshireCat> candidates = level.getEntitiesOfClass(EntityCheshireCat.class, CHESHIRE_BOUNDS,
                entity -> entity.isAlive() && !entity.isRemoved() && "map_051".equals(entity.getSceneId()));
        if (candidates.size() != 1 || !isCuratedCheshire(candidates.get(0))
                || !level.getEntitiesOfClass(EntityOriginalNpc.class, CHESHIRE_BOUNDS,
                entity -> entity.isAlive() && !entity.isRemoved() && sourceId.equals(entity.getNpcId())).isEmpty()) {
            throw new UnsupportedOperationException("animation_character_missing_or_ambiguous:" + sourceId);
        }
        return candidates.get(0);
    }

    public static boolean matches(Entity subject, String sourceId) {
        if (OriginalRabbitKnightBinding.SOURCE_ID.equals(sourceId) && subject instanceof yuki135.blacksouls.entity.EntityRabbitKnight) return OriginalRabbitKnightBinding.matches(subject);
        if (!CHESHIRE_RABBIT_HOLE.equals(sourceId)) {
            return subject instanceof EntityOriginalNpc original && original.getNpcId().equals(sourceId);
        }
        if (!(subject instanceof EntityCheshireCat) || !(subject.level() instanceof ServerLevel level)) return false;
        try {
            return resolve(level, sourceId) == subject;
        } catch (UnsupportedOperationException exception) {
            return false;
        }
    }

    public static boolean cheshireSourceVisible(EntityCheshireCat entity) {
        if (!isCuratedCheshire(entity)) return true;
        ServerLevel level = (ServerLevel) entity.level();
        OriginalNpcProgress progress = OriginalNpcProgress.get(level.getServer());
        ServerPlayer owner = OriginalNpcService.choosePageOwner(entity, CHESHIRE_RABBIT_HOLE, entity.forcedRouteTicket(),
                OriginalSenService.nearestPageOwner(entity));
        Integer cached = OriginalSenService.cachedPageThresholds(entity, CHESHIRE_RABBIT_HOLE);
        boolean zeroSen = owner != null ? OriginalNpcProgress.readSwitch(owner, 14)
                : cached == null ? progress.switchValue(14) : (cached & 1) != 0;
        return (progress.switchValue(109) || progress.switchValue(107) && !progress.switchValue(108))
                && !zeroSen && SceneSpawnerBossData.get(level.getServer()).getStoryProgress() < 1;
    }

    private static boolean isCuratedCheshire(EntityCheshireCat entity) {
        return "map_051".equals(entity.getSceneId())
                && Math.abs(entity.getX() - CHESHIRE_POSITION.x) <= 0.25D
                && Math.abs(entity.getY() - CHESHIRE_POSITION.y) <= 0.125D
                && Math.abs(entity.getZ() - CHESHIRE_POSITION.z) <= 0.25D
                && entity.level() instanceof ServerLevel level && OriginalNpcPlacements.hasDeployedMap(level);
    }

    private static OriginalNpcPlacements.Landmark landmark(int x, int y, int z, String block, Map<String, String> properties) {
        return new OriginalNpcPlacements.Landmark(new BlockPos(x, y, z), block, properties);
    }
}
