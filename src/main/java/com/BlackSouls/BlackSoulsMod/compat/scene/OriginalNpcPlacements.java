package com.BlackSouls.BlackSoulsMod.compat.scene;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityHolmes;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OriginalNpcPlacements {
    public static final BlockPos CEMETERY_BONFIRE = new BlockPos(786, 11, -607);
    private static final double NEARBY_DISTANCE_SQUARED = 64.0D * 64.0D;
    private static final List<Site> SITES = List.of(
            new Site("map002_ev059", new Vec3(615.5D, 13.0D, -531.5D), 180.0F,
                    "minecraft:stone_bricks", List.of(
                    landmark(618, 13, -530, "minecraft:stone_brick_stairs", Map.of("facing", "south", "half", "bottom")),
                    landmark(615, 13, -531, "minecraft:barrel", Map.of("facing", "up")),
                    landmark(615, 14, -531, "minecraft:candle", Map.of()),
                    landmark(616, 13, -532, "minecraft:cobblestone_wall", Map.of()))),
            new Site("map005_ev056", new Vec3(653.5D, 18.0D, -567.5D), 180.0F,
                    "minecraft:stone_bricks", List.of(
                    landmark(653, 13, -575, "minecraft:stone_brick_stairs", Map.of("facing", "south", "half", "bottom")),
                    landmark(653, 17, -571, "minecraft:stone_brick_stairs", Map.of("facing", "south", "half", "bottom")),
                    landmark(652, 18, -570, "minecraft:spruce_door", Map.of("facing", "west", "half", "lower")))),
            new Site("map005_ev017", new Vec3(580.5D, 13.0D, -575.5D), 90.0F,
                    "blacksouls2:xie_kuai", guardLandmarks()),
            new Site("map005_ev018", new Vec3(579.5D, 13.0D, -575.5D), 90.0F,
                    "blacksouls2:xie_kuai", guardLandmarks()),
            new Site("map006_ev051", new Vec3(89.5D, -57.0D, -1503.5D), 180.0F,
                    "minecraft:coarse_dirt", List.of(
                    landmark(92, -57, -1507, "blacksouls2:l_lu_deng_1", Map.of()),
                    landmark(92, -54, -1507, "blacksouls2:l_lu_deng_3", Map.of()),
                    landmark(84, -57, -1510, "blacksouls2:l_lu_deng_1", Map.of()),
                    landmark(100, -57, -1507, "blacksouls2:l_lu_deng_1", Map.of()))),
            new Site("map012_ev008", new Vec3(704.5D, -59.0D, -441.5D), 90.0F,
                    "blacksouls2:bai", wardLandmarks()),
            new Site("map012_ev009", new Vec3(715.5D, -58.4375D, -438.35D), 0.0F,
                    "cfm:oak_park_bench", wardSeatLandmarks(715, -439, "right"), true),
            new Site("map012_ev010", new Vec3(709.5D, -58.4375D, -442.35D), 0.0F,
                    "cfm:oak_park_bench", wardSeatLandmarks(709, -443, "left"), true),
            new Site("map012_ev011", new Vec3(717.5D, -58.4375D, -442.35D), 0.0F,
                    "cfm:oak_park_bench", wardSeatLandmarks(717, -443, "left"), true),
            new Site("map033_ev042", new Vec3(786.5D, 11.0D, -603.5D), 180.0F,
                    "minecraft:grass_block", cemeteryLandmarks()),
            new Site("map033_ev023", new Vec3(788.5D, 11.0D, -607.5D), 90.0F,
                    "minecraft:grass_block", cemeteryLandmarks()));

    private OriginalNpcPlacements() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.tickCount % 100 == 0 && !player.isSpectator()) {
            installNearby(player);
        }
    }

    public static List<Site> sites() {
        return SITES;
    }

    public static Map<String, Outcome> installNearby(ServerPlayer player) {
        if (!HokoniwaDestination.DIMENSION.equals(player.level().dimension())) {
            return Map.of();
        }
        Map<String, Outcome> results = new LinkedHashMap<>();
        for (Site site : SITES) {
            results.put(site.npcId(), install(player, site.npcId()));
        }
        results.put(OriginalReceptionistPlacement.NPC_ID, OriginalReceptionistPlacement.install(player));
        return Map.copyOf(results);
    }

    public static boolean hasDeployedMap(ServerLevel level) {
        return HokoniwaDestination.DIMENSION.equals(level.dimension()) && Files.isRegularFile(level.getServer()
                .getWorldPath(LevelResource.ROOT).resolve("dimensions").resolve("blacksouls")
                .resolve("hokoniwa").resolve("blacksouls_hakoniwa-v4.flag"));
    }

    public static boolean isOriginalRedHoodBonfire(ServerLevel level, BlockPos position) {
        return CEMETERY_BONFIRE.equals(position) && hasDeployedMap(level);
    }

    public static Outcome install(ServerPlayer player, String npcId) {
        if (OriginalReceptionistPlacement.NPC_ID.equals(npcId)) return OriginalReceptionistPlacement.install(player);
        Site site = SITES.stream().filter(value -> value.npcId().equals(npcId)).findFirst().orElse(null);
        if (site == null) {
            return Outcome.UNKNOWN_SITE;
        }
        ServerLevel level = player.serverLevel();
        if (!HokoniwaDestination.DIMENSION.equals(level.dimension())) {
            return Outcome.WRONG_DIMENSION;
        }
        if (!hasDeployedMap(level)) {
            return Outcome.MAP_NOT_DEPLOYED;
        }
        OriginalNpcPlacementData data = OriginalNpcPlacementData.get(level);
        if (data.isInstalled(npcId)) {
            return Outcome.ALREADY_RECORDED;
        }
        if (player.position().distanceToSqr(site.position()) > NEARBY_DISTANCE_SQUARED) {
            return Outcome.TOO_FAR;
        }
        if (!chunksReady(level, site)) {
            return Outcome.CHUNKS_UNAVAILABLE;
        }
        Entity existing = existingNpc(level, site);
        if (existing != null) {
            data.record(npcId, existing.getUUID());
            return Outcome.REUSED_EXISTING;
        }
        BlockPos floor = BlockPos.containing(site.position().add(0.0D, -0.02D, 0.0D));
        BlockState support = level.getBlockState(floor);
        if (!blockId(support).equals(site.supportBlock()) || !site.shapedSupport() && !support.isFaceSturdy(level, floor, Direction.UP)
                || site.landmarks().stream().anyMatch(landmark -> !landmark.matches(level))) {
            return Outcome.TERRAIN_MISMATCH;
        }
        EntityOriginalNpc npc = BSEntityRegistry.ORIGINAL_NPC.get().create(level);
        if (npc == null) {
            return Outcome.SPAWN_FAILED;
        }
        Vec3 position = site.position();
        npc.moveTo(position.x, position.y, position.z, site.yaw(), 0.0F);
        npc.setYHeadRot(site.yaw());
        npc.yBodyRot = site.yaw();
        npc.setNpcId(npcId);
        AABB bounds = npc.getBoundingBox();
        if (!level.noCollision(npc, bounds) || site.shapedSupport() && level.noCollision(npc, bounds.move(0.0D, -0.02D, 0.0D))
                || level.containsAnyLiquid(bounds)
                || !level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity.isAlive() && !entity.isSpectator()).isEmpty()) {
            npc.discard();
            return Outcome.COLLISION;
        }
        if (!level.addFreshEntity(npc)) {
            npc.discard();
            return Outcome.SPAWN_FAILED;
        }
        data.record(npcId, npc.getUUID());
        BlackSouls.LOGGER.info("Added original NPC {} at {} in {}", npcId, site.position(), level.dimension().location());
        return Outcome.INSTALLED;
    }

    public static Set<ChunkPos> requiredChunks(Site site) {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        AABB search = site.searchBounds();
        for (int x = BlockPos.containing(search.minX, 0.0D, 0.0D).getX() >> 4;
             x <= BlockPos.containing(search.maxX, 0.0D, 0.0D).getX() >> 4; x++) {
            for (int z = BlockPos.containing(0.0D, 0.0D, search.minZ).getZ() >> 4;
                 z <= BlockPos.containing(0.0D, 0.0D, search.maxZ).getZ() >> 4; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        for (Landmark landmark : site.landmarks()) {
            chunks.add(new ChunkPos(landmark.position()));
        }
        return Set.copyOf(chunks);
    }

    public static boolean chunksReady(ServerLevel level, Site site) {
        for (ChunkPos chunk : requiredChunks(site)) {
            if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null || !level.areEntitiesLoaded(chunk.toLong())) {
                return false;
            }
        }
        return true;
    }

    private static Entity existingNpc(ServerLevel level, Site site) {
        List<EntityOriginalNpc> generic = level.getEntitiesOfClass(EntityOriginalNpc.class, site.searchBounds(),
                entity -> !entity.isRemoved() && site.npcId().equals(entity.getNpcId()));
        if (!generic.isEmpty()) {
            return generic.get(0);
        }
        if (site.npcId().equals("map002_ev059")) {
            List<EntityHolmes> legacy = level.getEntitiesOfClass(EntityHolmes.class, site.searchBounds(),
                    entity -> !entity.isRemoved());
            if (!legacy.isEmpty()) {
                return legacy.get(0);
            }
        }
        return null;
    }

    private static List<Landmark> guardLandmarks() {
        return List.of(
                landmark(581, 13, -576, "minecraft:spruce_door", Map.of("facing", "east", "half", "lower")),
                landmark(577, 13, -577, "minecraft:spruce_door", Map.of("facing", "south", "half", "lower")),
                landmark(580, 12, -570, "minecraft:stone_bricks", Map.of()));
    }

    private static List<Landmark> cemeteryLandmarks() {
        return List.of(
                landmark(786, 11, -607, "minecraft:campfire", Map.of("facing", "east")),
                landmark(786, 11, -603, "blacksouls2:l_lu_deng_1", Map.of("facing", "north")),
                landmark(786, 14, -603, "blacksouls2:l_lu_deng_3", Map.of("facing", "north")),
                landmark(788, 11, -605, "minecraft:oak_log", Map.of("axis", "y")),
                landmark(788, 12, -605, "moa_decor_garden_:hachaclavada", Map.of("facing", "up")),
                landmark(784, 11, -610, "blacksouls2:sh_sheng_zi", Map.of("facing", "north")));
    }

    private static List<Landmark> wardSeatLandmarks(int x, int z, String type) {
        java.util.ArrayList<Landmark> landmarks = new java.util.ArrayList<>(wardLandmarks());
        landmarks.add(landmark(x, -59, z, "cfm:oak_park_bench", Map.of("facing", "north", "type", type)));
        return List.copyOf(landmarks);
    }

    private static List<Landmark> wardLandmarks() {
        return List.of(
                landmark(716, -59, -435, "mcwfurnitures:spruce_covered_desk", Map.of("facing", "east", "connection", "right")),
                landmark(708, -59, -435, "mcwfurnitures:spruce_covered_desk", Map.of("facing", "east", "connection", "left")),
                landmark(712, -59, -441, "blacksouls2:pen_zai", Map.of("facing", "south")),
                landmark(705, -59, -432, "minecraft:dark_oak_door", Map.of("facing", "south", "half", "lower")),
                landmark(719, -59, -432, "minecraft:dark_oak_door", Map.of("facing", "south", "half", "lower")));
    }

    private static Landmark landmark(int x, int y, int z, String blockId, Map<String, String> properties) {
        return new Landmark(new BlockPos(x, y, z), blockId, properties);
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    public record Site(String npcId, Vec3 position, float yaw, String supportBlock, List<Landmark> landmarks, boolean shapedSupport) {
        public Site(String npcId, Vec3 position, float yaw, String supportBlock, List<Landmark> landmarks) {
            this(npcId, position, yaw, supportBlock, landmarks, false);
        }

        public Site {
            landmarks = List.copyOf(landmarks);
        }

        public AABB searchBounds() {
            return new AABB(position, position).inflate(12.0D, 16.0D, 12.0D);
        }
    }

    public record Landmark(BlockPos position, String blockId, Map<String, String> properties) {
        public Landmark {
            position = position.immutable();
            properties = Map.copyOf(properties);
        }

        public boolean matches(ServerLevel level) {
            BlockState state = level.getBlockState(position);
            if (!OriginalNpcPlacements.blockId(state).equals(blockId)) {
                return false;
            }
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
                if (property == null || !propertyName(state, property).equals(entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
    }

    private static <T extends Comparable<T>> String propertyName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    public enum Outcome {
        RECOVERED_MIGRATION, INSTALLED, REUSED_EXISTING, ALREADY_RECORDED, UNKNOWN_SITE, WRONG_DIMENSION,
        MAP_NOT_DEPLOYED, TOO_FAR, CHUNKS_UNAVAILABLE, TERRAIN_MISMATCH, COLLISION, SPAWN_FAILED
    }
}
