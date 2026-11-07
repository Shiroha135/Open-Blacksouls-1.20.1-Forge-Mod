package yuki135.blacksouls.compat.scene;

import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.handler.BSEntityRegistry;
import yuki135.blacksouls.story.OriginalNpcInterpreter;
import yuki135.blacksouls.story.OriginalNpcService;
import yuki135.blacksouls.story.OriginalSenService;
import yuki135.blacksouls.util.BSOriginalNpcData;
import yuki135.blacksouls.util.HokoniwaDestination;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OriginalPoolCheshirePlacement {
    public static final String PROXY_ID = "map037_ev075";
    public static final String BODY_ID = "map037_ev076";
    public static final double FEET_Y = 18.0D;
    private static final String MANAGED = "BlacksoulsOriginalPoolCheshire";
    private static final String GRAVITY_CAPTURED = "BlacksoulsOriginalPoolCheshireGravityCaptured";
    private static final String ORIGINAL_NO_GRAVITY = "BlacksoulsOriginalPoolCheshireOriginalNoGravity";
    private static final String BLOOD_WATER = "blacksouls2:xie_shui";
    private static final List<OriginalNpcPlacements.Landmark> LANDMARKS = List.of(
            new OriginalNpcPlacements.Landmark(new BlockPos(861, 18, -568), "mcwtrpdoors:jungle_cottage_trapdoor",
                    Map.of("facing", "north", "half", "bottom")),
            new OriginalNpcPlacements.Landmark(new BlockPos(861, 18, -569), "mcwtrpdoors:jungle_cottage_trapdoor",
                    Map.of("facing", "north", "half", "bottom")),
            new OriginalNpcPlacements.Landmark(new BlockPos(862, 18, -569), "minecraft:campfire",
                    Map.of("facing", "west")));
    private static final List<OriginalNpcPlacements.Site> SITES = List.of(
            new OriginalNpcPlacements.Site(PROXY_ID, new Vec3(861.5D, FEET_Y, -566.5D), 180.0F, BLOOD_WATER, LANDMARKS),
            new OriginalNpcPlacements.Site(BODY_ID, new Vec3(861.5D, FEET_Y, -565.5D), 180.0F, BLOOD_WATER, LANDMARKS));

    private OriginalPoolCheshirePlacement() {
    }

    public static boolean isPair(String identity) {
        return PROXY_ID.equals(identity) || BODY_ID.equals(identity);
    }

    public static boolean isManaged(EntityOriginalNpc actor) {
        return isPair(actor.getNpcId()) && actor.getPersistentData().getBoolean(MANAGED);
    }

    public static boolean isOwnedProxy(EntityOriginalNpc actor) {
        if (!PROXY_ID.equals(actor.getNpcId()) || !isManaged(actor) || !actor.isAlive() || actor.isRemoved()
                || !(actor.level() instanceof ServerLevel level) || !HokoniwaDestination.DIMENSION.equals(level.dimension())
                || actor.position().distanceToSqr(SITES.get(0).position()) > 1.0E-8D) return false;
        return actor.getUUID().equals(OriginalNpcPlacementData.get(level).entityId(PROXY_ID));
    }

    public static boolean interactStaticTile(ServerPlayer player, BlockPos position) {
        if (!OriginalPoolTile65Placement.POSITION.equals(position) || !player.isAlive() || player.isSpectator()
                || player.distanceToSqr(position.getCenter()) > 64.0D || !terrainMatches(player.serverLevel())
                || !OriginalPoolTile65Placement.isInstalled(player.serverLevel())) return false;
        install(player, PROXY_ID);
        Pair pair = resolve(player.serverLevel());
        if (pair == null || interactionProxy(player, pair.proxy()) == null) return false;
        OriginalNpcService.interact(player, pair.proxy());
        return true;
    }

    public static List<OriginalNpcPlacements.Site> sites() {
        return SITES;
    }

    public static OriginalNpcPlacements.Site site(String identity) {
        return SITES.stream().filter(value -> value.npcId().equals(identity)).findFirst().orElse(null);
    }

    public static Set<ChunkPos> requiredChunks() {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        for (var site : SITES) chunks.addAll(OriginalNpcPlacements.requiredChunks(site));
        return Set.copyOf(chunks);
    }

    private static boolean chunksReady(ServerLevel level) {
        for (ChunkPos chunk : requiredChunks()) {
            if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null || !level.areEntitiesLoaded(chunk.toLong())) return false;
        }
        return true;
    }

    public static Set<ChunkPos> runtimeRequiredChunks() {
        return Set.of(new ChunkPos(53, -36));
    }

    private static boolean runtimeChunksReady(ServerLevel level) {
        for (ChunkPos chunk : runtimeRequiredChunks()) {
            if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null || !level.areEntitiesLoaded(chunk.toLong())) return false;
        }
        return true;
    }

    public static boolean terrainMatches(ServerLevel level) {
        if (!OriginalNpcPlacements.hasDeployedMap(level) || !runtimeChunksReady(level)
                || LANDMARKS.stream().anyMatch(mark -> !mark.matches(level))) return false;
        for (var site : SITES) {
            BlockPos water = BlockPos.containing(site.position()).below();
            BlockPos bottom = water.below();
            var state = level.getBlockState(water);
            var fluid = state.getFluidState();
            if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(BLOOD_WATER)
                    || fluid.isEmpty() || !fluid.isSource() || !level.getBlockState(bottom).is(Blocks.COARSE_DIRT)
                    || !level.getBlockState(bottom).isFaceSturdy(level, bottom, Direction.UP)) return false;
        }
        return true;
    }

    private static List<EntityOriginalNpc> candidates(ServerLevel level, OriginalNpcPlacements.Site site) {
        return level.getEntitiesOfClass(EntityOriginalNpc.class, site.searchBounds(),
                entity -> !entity.isRemoved() && site.npcId().equals(entity.getNpcId()));
    }

    private static boolean validActor(EntityOriginalNpc actor, OriginalNpcPlacements.Site site, OriginalNpcPlacementData data) {
        return actor.isAlive() && !actor.isRemoved() && actor.getPersistentData().getBoolean(MANAGED)
                && actor.getNpcId().equals(site.npcId()) && actor.position().distanceToSqr(site.position()) <= 1.0E-8D
                && actor.getUUID().equals(data.entityId(site.npcId()));
    }

    private static Pair resolve(ServerLevel level) {
        if (!OriginalNpcPlacements.hasDeployedMap(level) || !runtimeChunksReady(level)) return null;
        OriginalNpcPlacementData data = OriginalNpcPlacementData.get(level);
        List<EntityOriginalNpc> proxies = candidates(level, SITES.get(0));
        List<EntityOriginalNpc> bodies = candidates(level, SITES.get(1));
        if (proxies.size() != 1 || bodies.size() != 1 || !validActor(proxies.get(0), SITES.get(0), data)
                || !validActor(bodies.get(0), SITES.get(1), data)) return null;
        return new Pair(proxies.get(0), bodies.get(0));
    }

    private static boolean clear(ServerLevel level, EntityOriginalNpc actor, boolean entities) {
        AABB bounds = actor.getBoundingBox();
        if (level.containsAnyLiquid(bounds)) return false;
        boolean ownTile = PROXY_ID.equals(actor.getNpcId()) && actor.isStaticMapProxy()
                && OriginalPoolTile65Placement.isInstalled(level);
        for (var shape : level.getBlockCollisions(actor, bounds)) {
            if (!shape.isEmpty() && !(ownTile && shape.bounds().equals(OriginalPoolTile65Placement.VOLUME))) return false;
        }
        return !entities || level.getEntityCollisions(actor, bounds).isEmpty() && level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != actor && entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

    public static OriginalNpcPlacements.Outcome install(ServerPlayer player, String identity) {
        if (!isPair(identity)) return OriginalNpcPlacements.Outcome.UNKNOWN_SITE;
        ServerLevel level = player.serverLevel();
        if (!HokoniwaDestination.DIMENSION.equals(level.dimension())) return OriginalNpcPlacements.Outcome.WRONG_DIMENSION;
        if (!OriginalNpcPlacements.hasDeployedMap(level)) return OriginalNpcPlacements.Outcome.MAP_NOT_DEPLOYED;
        if (player.position().distanceToSqr(site(identity).position()) > 4096.0D) return OriginalNpcPlacements.Outcome.TOO_FAR;
        if (!chunksReady(level)) return OriginalNpcPlacements.Outcome.CHUNKS_UNAVAILABLE;
        if (!terrainMatches(level)) return OriginalNpcPlacements.Outcome.TERRAIN_MISMATCH;
        var tile = OriginalPoolTile65Placement.install(player);
        if (tile != OriginalPoolTile65Placement.Outcome.INSTALLED && tile != OriginalPoolTile65Placement.Outcome.ALREADY_RECORDED) {
            return OriginalNpcPlacements.Outcome.COLLISION;
        }
        OriginalNpcPlacementData data = OriginalNpcPlacementData.get(level);
        if (data.isInstalled(PROXY_ID) || data.isInstalled(BODY_ID)) {
            Pair pair = resolve(level);
            if (pair == null) return OriginalNpcPlacements.Outcome.COLLISION;
            pair.proxy().setStaticMapProxy(true);
            return OriginalNpcPlacements.Outcome.ALREADY_RECORDED;
        }
        if (SITES.stream().anyMatch(site -> !candidates(level, site).isEmpty())) return OriginalNpcPlacements.Outcome.COLLISION;
        EntityOriginalNpc proxy = create(level, SITES.get(0));
        EntityOriginalNpc body = create(level, SITES.get(1));
        if (proxy == null || body == null) {
            discard(proxy, body);
            return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        }
        if (!clear(level, proxy, true) || !clear(level, body, true) || proxy.getBoundingBox().intersects(body.getBoundingBox())) {
            discard(proxy, body);
            return OriginalNpcPlacements.Outcome.COLLISION;
        }
        try {
            if (!level.addFreshEntity(proxy) || !level.addFreshEntity(body)
                    || data.isInstalled(PROXY_ID) || data.isInstalled(BODY_ID)
                    || candidates(level, SITES.get(0)).size() != 1 || candidates(level, SITES.get(1)).size() != 1) {
                discard(proxy, body);
                return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
            }
            data.record(PROXY_ID, proxy.getUUID());
            data.record(BODY_ID, body.getUUID());
        } catch (RuntimeException failure) {
            discard(proxy, body);
            return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        }
        beforeEntityTick(proxy);
        beforeEntityTick(body);
        refreshPage(proxy, player);
        return OriginalNpcPlacements.Outcome.INSTALLED;
    }

    private static EntityOriginalNpc create(ServerLevel level, OriginalNpcPlacements.Site site) {
        EntityOriginalNpc actor = BSEntityRegistry.ORIGINAL_NPC.get().create(level);
        if (actor == null) return null;
        actor.moveTo(site.position().x, site.position().y, site.position().z, site.yaw(), 0.0F);
        actor.setYHeadRot(site.yaw());
        actor.yBodyRot = site.yaw();
        actor.setNpcId(site.npcId());
        actor.getPersistentData().putBoolean(MANAGED, true);
        if (PROXY_ID.equals(site.npcId())) actor.setStaticMapProxy(true);
        return actor;
    }

    private static void discard(EntityOriginalNpc proxy, EntityOriginalNpc body) {
        if (proxy != null) proxy.discard();
        if (body != null) body.discard();
    }

    public static EntityOriginalNpc interactionProxy(ServerPlayer player, EntityOriginalNpc actor) {
        if (!isPair(actor.getNpcId()) || player.level() != actor.level() || !(actor.level() instanceof ServerLevel level)) return null;
        Pair pair = resolve(level);
        if (pair == null || actor != pair.proxy() && actor != pair.body() || !terrainMatches(level)
                || !clear(level, pair.proxy(), false) || !clear(level, pair.body(), false)) return null;
        refreshPage(pair.proxy(), player);
        return pair.proxy();
    }

    public static boolean refreshPage(EntityOriginalNpc actor, ServerPlayer requested) {
        if (!isPair(actor.getNpcId()) || !(actor.level() instanceof ServerLevel level)) return false;
        Pair pair = resolve(level);
        if (pair == null || actor != pair.proxy() && actor != pair.body()) return false;
        ServerPlayer fallback = requested != null && requested.level() == level && requested.isAlive() && !requested.isSpectator()
                && requested.distanceToSqr(pair.proxy()) <= 1024.0D ? requested : OriginalSenService.nearestPageOwner(pair.proxy());
        OriginalNpcProgress progress = OriginalNpcProgress.get(level.getServer());
        List<ServerPlayer> savedOwners = level.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == level && player.isAlive() && !player.isSpectator()
                        && hasSavedOwner(progress, player, pair.proxy())).toList();
        if (savedOwners.size() == 1) fallback = savedOwners.get(0);
        else if (savedOwners.size() > 1) fallback = null;
        ServerPlayer owner = OriginalNpcService.choosePageOwner(pair.proxy(), PROXY_ID, pair.proxy().forcedRouteTicket(), fallback);
        if (owner != null) {
            OriginalSenService.rememberPageOwner(pair.proxy(), owner);
            OriginalSenService.rememberPageOwner(pair.body(), owner);
        } else OriginalSenService.copyPageOwner(pair.proxy(), PROXY_ID, pair.body(), BODY_ID);
        for (EntityOriginalNpc target : List.of(pair.proxy(), pair.body())) {
            var definition = BSOriginalNpcData.getEvent(target.getNpcId());
            var page = OriginalNpcInterpreter.selectPage(definition, new OriginalNpcService.EventContext(owner, target));
            target.setPageIndex(page == null ? -1 : definition.pages().indexOf(page));
        }
        return true;
    }

    private static boolean hasSavedOwner(OriginalNpcProgress progress, ServerPlayer player, EntityOriginalNpc proxy) {
        var saved = progress.conversation(player.getUUID(), PROXY_ID);
        return saved != null && saved.has("entityUuid") && saved.has("dimension")
                && proxy.getUUID().toString().equals(saved.get("entityUuid").getAsString())
                && proxy.level().dimension().location().toString().equals(saved.get("dimension").getAsString());
    }

    public static void beforeEntityTick(EntityOriginalNpc actor) {
        if (!(actor.level() instanceof ServerLevel level)) return;
        Pair pair = isPair(actor.getNpcId()) ? resolve(level) : null;
        boolean supported = pair != null && (actor == pair.proxy() || actor == pair.body())
                && terrainMatches(level) && clear(level, actor, false);
        var data = actor.getPersistentData();
        if (supported) {
            if (!data.getBoolean(GRAVITY_CAPTURED)) {
                data.putBoolean(GRAVITY_CAPTURED, true);
                data.putBoolean(ORIGINAL_NO_GRAVITY, actor.isNoGravity());
            }
            actor.setNoGravity(true);
            actor.setDeltaMovement(Vec3.ZERO);
            actor.fallDistance = 0;
        } else if (data.getBoolean(GRAVITY_CAPTURED)) {
            actor.setNoGravity(data.getBoolean(ORIGINAL_NO_GRAVITY));
            data.remove(GRAVITY_CAPTURED);
            data.remove(ORIGINAL_NO_GRAVITY);
        }
    }

    private record Pair(EntityOriginalNpc proxy, EntityOriginalNpc body) {
    }
}
