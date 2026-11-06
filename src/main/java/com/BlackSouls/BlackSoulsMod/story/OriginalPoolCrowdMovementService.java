package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.OriginalNpcProgress;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OriginalPoolCrowdMovementService {
    private static final List<String> ORDER = List.of("map037_ev004", "map037_ev003", "map037_ev005", "map037_ev006", "map037_ev007",
            "map037_ev008", "map037_ev009", "map037_ev010", "map037_ev011", "map037_ev054");
    private static final JsonObject SOURCE = loadSource();
    private static final byte[] PASSAGE = Base64.getDecoder().decode(SOURCE.get("basePassageMasks").getAsString());
    private static final byte[] BUSH = Base64.getDecoder().decode("xwEAAAAAAACOAwAAAAAAABwHAAAAAAAAOA4AAAAAAABwfPz////////4+P////////Hx////////4+P////////Hx////////4+P////////Hx////////8/Pv7//////398/P//f/z///gAAOD/+P//8QEAwP/////jAwCA/////8f//x//////j///P/7///8f//9//P//Hzj+///4//8PcPz///H//wfg+P//4///B8Dx//8HAAAAgOP//w8AAAAAx///HwAAAACO////////ABz///////8POP7///////9z/P/////////4//////////H///////9/");
    private static final byte[] WATER = Base64.getDecoder().decode("xwEAAAAAAACOAwAAAAAAABwHAAAAAAAAOA4AAAAAAABwfPz////////4+P////////Hx////////4+P////////Hx////////4+P////////Hx////////8/Pv7//////398/P//f/z///gAAOD/+P//8QEAwP/////jAwCA/////8f//x//////j///P/7///8f//9//P//Hzj+///4//8PcPz///H//wfg+P//4///B8Dx//8HAAAAgOP//w8AAAAAx///HwAAAACO////////ABz///////8POP7///////9z/P/////////4//////////H///////9/");
    private static final Map<EntityOriginalNpc, String> SURFACE_WARNINGS = new WeakHashMap<>();
    private static final ThreadLocal<SourceActors> SHARED_ACTORS = new ThreadLocal<>();

    private OriginalPoolCrowdMovementService() {
    }

    public static boolean usesSharedScheduler(EntityOriginalNpc npc) {
        if (!OriginalPoolCrowdSurface.isCrowd(npc.getNpcId()) || !(npc.level() instanceof ServerLevel level)
                || !level.dimension().equals(HokoniwaDestination.DIMENSION) || !OriginalPoolCrowdSurface.isLowerPoolPosition(npc.getX(), npc.getZ())) return false;
        if (npc.originalMovementState().getBoolean("PoolSurfaceWasActive")) return true;
        var motion = OriginalNpcMovementService.originalMotion(npc);
        if (motion != null) {
            var state = motion.snapshot();
            var start = new Vec3(state.get("startX").getAsDouble(), state.get("startY").getAsDouble(), state.get("startZ").getAsDouble());
            if (supportedAt(npc, start, npc.getBoundingBox().move(start.subtract(npc.position())))) return true;
        }
        return supportedAt(npc, npc.position(), npc.getBoundingBox());
    }

    public static int sourceOrder(String identity) {
        return ORDER.indexOf(identity);
    }

    public static boolean sourceBush(int x, int y) {
        return valid(x, y) && (BUSH[((y - 34) * 65 + x) / 8] & (1 << (((y - 34) * 65 + x) % 8))) != 0;
    }

    private static boolean sourceWater(int x, int y) {
        return valid(x, y) && (WATER[((y - 34) * 65 + x) / 8] & (1 << (((y - 34) * 65 + x) % 8))) != 0;
    }

    static void tick(MinecraftServer server, OriginalNpcMovementData data) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.dimension().equals(HokoniwaDestination.DIMENSION)) continue;
            List<EntityOriginalNpc> loaded = sourceActors(level);
            List<EntityOriginalNpc> actors = loaded.stream().filter(npc -> usesSharedScheduler(npc)
                    && level.hasChunkAt(npc.blockPosition())).sorted(Comparator.comparingInt((EntityOriginalNpc npc) -> sourceOrder(npc.getNpcId()))
                    .thenComparing(Entity::getUUID)).toList();
            SHARED_ACTORS.set(new SourceActors(level, loaded));
            try {
                for (int frame = 0; frame < 3; frame++) {
                    for (EntityOriginalNpc npc : actors) {
                        if (!OriginalPoolCrowdVisitService.ready(npc) || !prepareSurface(npc)) continue;
                        ServerPlayer owner = nearestContext(npc);
                        OriginalNpcMovementService.advanceCrowdFrame(npc, owner, data);
                        refreshBush(npc);
                    }
                }
            } finally { SHARED_ACTORS.remove(); }
        }
    }

    public static ServerPlayer nearestContext(EntityOriginalNpc npc) {
        if (!(npc.level() instanceof ServerLevel level)) return null;
        return level.getServer().getPlayerList().getPlayers().stream().filter(player -> player.serverLevel() == level && player.isAlive() && !player.isSpectator())
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(npc))).orElse(null);
    }

    public static void beforeEntityTick(EntityOriginalNpc npc) {
        if (!usesSharedScheduler(npc)) { npc.applyOriginalPoolSurfaceSupport(false); return; }
        if (!OriginalPoolCrowdVisitService.ready(npc)) return;
        prepareSurface(npc);
    }

    private static boolean prepareSurface(EntityOriginalNpc npc) {
        Vec3 logical = OriginalNpcMovementService.logicalPosition(npc);
        var motion = OriginalNpcMovementService.originalMotion(npc);
        double ground = motion == null ? npc.getY() : motion.snapshot().get("startY").getAsDouble();
        AABB bounds = npc.getBoundingBox().move(0, ground - npc.getY(), 0);
        boolean supported = supportedAt(npc, new Vec3(npc.getX(), ground, npc.getZ()), bounds)
                && Double.isFinite(supportHeight(npc, logical.x, logical.z));
        npc.applyOriginalPoolSurfaceSupport(supported);
        if (!supported) {
            String reason = "source=" + (885 - BlockPos.containing(logical).getX()) + "," + (-549 - BlockPos.containing(logical).getZ())
                    + " actual=" + npc.position();
            if (SURFACE_WARNINGS.putIfAbsent(npc, reason) == null) BlackSouls.LOGGER.warn("Original pool crowd {} paused for mismatched terrain: {}", npc.getNpcId(), reason);
        } else SURFACE_WARNINGS.remove(npc);
        return supported;
    }

    public static void refreshBush(EntityOriginalNpc npc) {
        if (!OriginalPoolCrowdSurface.isCrowd(npc.getNpcId())) return;
        Vec3 logical = OriginalNpcMovementService.logicalPosition(npc);
        var motion = OriginalNpcMovementService.originalMotion(npc);
        boolean jumping = motion != null && motion.snapshot().get("jump").getAsBoolean();
        boolean moving = motion != null && (motion.x() != logical.x || motion.z() != logical.z);
        boolean object = npc.getRouteTileId() > 0 || npc.getRouteCharacterName().startsWith("!");
        var page = npc.currentPage();
        boolean normal = page != null && page.movement().has("priority_type") && page.movement().get("priority_type").getAsInt() == 1;
        int x = 885 - BlockPos.containing(logical).getX(), y = -549 - BlockPos.containing(logical).getZ();
        int depth = normal && !object && sourceBush(x, y) && !jumping ? moving ? npc.getOriginalBushDepth() : 8 : 0;
        npc.setOriginalBushState(depth, object ? 0 : 4);
    }

    public static boolean allowsSourceStep(EntityOriginalNpc npc, ServerPlayer owner, int x, int y, int direction) {
        if (!OriginalPoolCrowdSurface.isCrowd(npc.getNpcId()) && !npc.getNpcId().equals("map037_ev053")) return false;
        int dx = direction == 4 ? -1 : direction == 6 ? 1 : 0;
        int dy = direction == 8 ? -1 : direction == 2 ? 1 : 0;
        if (!List.of(2, 4, 6, 8).contains(direction) || !valid(x, y) || !valid(x + dx, y + dy)) return false;
        if (npc.originalMovementState().getBoolean("Through")) return true;
        return passage(npc, owner, x, y, direction) && passage(npc, owner, x + dx, y + dy, 10 - direction)
                && !blocksSourceTile(npc, owner, x + dx, y + dy);
    }

    public static boolean blocksSourceTile(EntityOriginalNpc moving, ServerPlayer owner, int x, int y) {
        if (!(moving.level() instanceof ServerLevel level)) return true;
        List<EntityOriginalNpc> actual = sourceActors(level);
        for (EntityOriginalNpc other : actual) {
            if (other == moving || other.isRemoved() || !other.isAlive() || other.isErasedForCurrentVisit() || other.currentPage() == null || other.originalMovementState().getBoolean("Through")) continue;
            var movement = other.currentPage().movement();
            if (movement.get("priority_type").getAsInt() != 1) continue;
            Vec3 position = OriginalNpcMovementService.logicalPosition(other);
            if (885 - BlockPos.containing(position).getX() == x && -549 - BlockPos.containing(position).getZ() == y) return true;
        }
        for (var raw : SOURCE.getAsJsonArray("events")) {
            var event = raw.getAsJsonObject();
            String identity = "map037_ev%03d".formatted(event.get("id").getAsInt());
            if (identity.equals(moving.getNpcId()) || actual.stream().anyMatch(actor -> actor.getNpcId().equals(identity))
                    || event.get("x").getAsInt() != x || event.get("y").getAsInt() != y) continue;
            JsonObject page = selected(moving, owner, event);
            if (page != null && !page.get("through").getAsBoolean() && page.get("priority").getAsInt() == 1) return true;
        }
        if (moving.currentPage() != null && moving.currentPage().movement().get("priority_type").getAsInt() == 1) {
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                if (player.serverLevel() == level && !player.isSpectator() && player.isAlive() && 885 - player.blockPosition().getX() == x
                        && -549 - player.blockPosition().getZ() == y && Math.abs(player.getY() - moving.getY()) < 1.0D) return true;
            }
        }
        return false;
    }

    private static boolean passage(EntityOriginalNpc actor, ServerPlayer owner, int x, int y, int direction) {
        int bit = 1 << (direction / 2 - 1);
        var actual = sourceActors((ServerLevel) actor.level());
        for (var raw : SOURCE.getAsJsonArray("events")) {
            var event = raw.getAsJsonObject();
            String identity = "map037_ev%03d".formatted(event.get("id").getAsInt());
            var loaded = actual.stream().filter(npc -> npc.getNpcId().equals(identity)).findFirst().orElse(null);
            if (loaded != null) {
                var position = OriginalNpcMovementService.logicalPosition(loaded);
                if (loaded.isErasedForCurrentVisit() || 885 - BlockPos.containing(position).getX() != x
                        || -549 - BlockPos.containing(position).getZ() != y || loaded.currentPage() == null
                        || loaded.originalMovementState().getBoolean("Through") || loaded.getRouteTileId() == 0
                        || loaded.currentPage().movement().get("priority_type").getAsInt() != 0) continue;
                var definition = BSOriginalNpcData.getEvent(identity);
                int index = definition == null ? -1 : definition.pages().indexOf(loaded.currentPage());
                if (index < 0 || index >= event.getAsJsonArray("pages").size()) continue;
                int flag = event.getAsJsonArray("pages").get(index).getAsJsonObject().get("tileFlag").getAsInt();
                if ((flag & 0x10) == 0) return (flag & bit) == 0;
            } else {
                if (event.get("x").getAsInt() != x || event.get("y").getAsInt() != y) continue;
                var page = selected(actor, owner, event);
                if (page == null || page.get("through").getAsBoolean() || page.get("priority").getAsInt() != 0 || page.get("tileId").getAsInt() == 0) continue;
                int flag = page.get("tileFlag").getAsInt();
                if ((flag & 0x10) == 0) return (flag & bit) == 0;
            }
        }
        return (PASSAGE[(y - 34) * 65 + x] & bit) != 0;
    }

    private static JsonObject selected(EntityOriginalNpc actor, ServerPlayer owner, JsonObject event) {
        var progress = OriginalNpcProgress.get(actor.level().getServer());
        var context = new OriginalNpcService.EventContext(owner, actor);
        var pages = event.getAsJsonArray("pages");
        String identity = "map037_ev%03d".formatted(event.get("id").getAsInt());
        for (int index = pages.size() - 1; index >= 0; index--) {
            var page = pages.get(index).getAsJsonObject();
            var c = page.getAsJsonObject("condition");
            if (c.get("switch1_valid").getAsBoolean() && !(context == null ? progress.switchValue(c.get("switch1_id").getAsInt()) : context.switchValue(c.get("switch1_id").getAsInt()))) continue;
            if (c.get("switch2_valid").getAsBoolean() && !(context == null ? progress.switchValue(c.get("switch2_id").getAsInt()) : context.switchValue(c.get("switch2_id").getAsInt()))) continue;
            if (c.get("variable_valid").getAsBoolean() && (context == null ? progress.variable(c.get("variable_id").getAsInt()) : context.variable(c.get("variable_id").getAsInt())) < c.get("variable_value").getAsLong()) continue;
            if (c.get("self_switch_valid").getAsBoolean() && !progress.selfSwitch(identity, c.get("self_switch_ch").getAsString())) continue;
            if (c.get("item_valid").getAsBoolean() && (context == null || context.itemCount(0, c.get("item_id").getAsInt()) <= 0)) continue;
            if (c.get("actor_valid").getAsBoolean() && (context == null || !context.hasActor(c.get("actor_id").getAsInt()))) continue;
            return page;
        }
        return null;
    }

    private static List<EntityOriginalNpc> sourceActors(ServerLevel level) {
        SourceActors shared = SHARED_ACTORS.get();
        if (shared != null && shared.level() == level) return shared.actors();
        List<EntityOriginalNpc> result = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof EntityOriginalNpc npc && npc.isAlive() && !npc.isRemoved()) {
                var source = BSOriginalNpcData.getEvent(npc.getNpcId());
                if (source != null && source.mapId() == 37 && OriginalPoolCrowdSurface.isLowerPoolPosition(npc.getX(), npc.getZ())) result.add(npc);
            }
        }
        return result;
    }

    public static double supportHeight(EntityOriginalNpc npc, double x, double z) {
        if (!(npc.level() instanceof ServerLevel level) || !level.dimension().equals(HokoniwaDestination.DIMENSION)
                || !OriginalPoolCrowdSurface.isLowerPoolPosition(x, z)) return Double.NaN;
        int sourceX = 885 - BlockPos.containing(x, 0, z).getX(), sourceY = -549 - BlockPos.containing(x, 0, z).getZ();
        var bounds = npc.getBoundingBox().move(x - npc.getX(), 11.0D - npc.getY(), z - npc.getZ());
        if (OriginalPoolCrowdSurface.supports(level, npc.getNpcId(), new Vec3(x, 11.0D, z), bounds) && clearForeign(npc, bounds)) return 11.0D;
        if (!valid(sourceX, sourceY) || sourceWater(sourceX, sourceY) || (PASSAGE[(sourceY - 34) * 65 + sourceX] & 15) == 0) return Double.NaN;
        var motion = OriginalNpcMovementService.originalMotion(npc);
        double baseline = motion == null ? npc.getY() : motion.snapshot().get("targetY").getAsDouble();
        var scan = new AABB(bounds.minX, baseline - 1.05D, bounds.minZ, bounds.maxX, baseline + 0.75D, bounds.maxZ);
        double selected = Double.NaN, distance = Double.MAX_VALUE;
        for (var shape : level.getBlockCollisions(null, scan)) for (var box : shape.toAabbs()) {
            double y = box.maxY;
            if (y < baseline - 1.0D || y > baseline + 0.75D) continue;
            var candidate = bounds.move(0, y - 11.0D, 0);
            if (!solidSupported(level, candidate) || !clearForeign(npc, candidate)) continue;
            if (Math.abs(y - baseline) < distance) { selected = y; distance = Math.abs(y - baseline); }
        }
        return selected;
    }

    private static boolean supportedAt(EntityOriginalNpc npc, Vec3 position, AABB bounds) {
        if (!(npc.level() instanceof ServerLevel level)) return false;
        if (OriginalPoolCrowdSurface.supports(level, npc.getNpcId(), position, bounds)) return clearForeign(npc, bounds);
        int x = 885 - BlockPos.containing(position).getX(), y = -549 - BlockPos.containing(position).getZ();
        return level.dimension().equals(HokoniwaDestination.DIMENSION) && valid(x, y) && !sourceWater(x, y)
                && (PASSAGE[(y - 34) * 65 + x] & 15) != 0 && solidSupported(level, bounds) && clearForeign(npc, bounds);
    }

    private static boolean solidSupported(ServerLevel level, AABB bounds) {
        if (level.containsAnyLiquid(bounds)) return false;
        boolean support = false;
        for (var shape : level.getBlockCollisions(null, bounds)) if (!shape.isEmpty()) return false;
        for (var shape : level.getBlockCollisions(null, bounds.move(0, -0.02D, 0))) if (!shape.isEmpty()) support = true;
        return support;
    }

    private static boolean clearForeign(EntityOriginalNpc npc, AABB bounds) {
        return npc.level().getEntities(npc, bounds, other -> !other.isSpectator() && (npc.canCollideWith(other) || other.canBeCollidedWith()) && !(other instanceof EntityOriginalNpc original
                && BSOriginalNpcData.getEvent(original.getNpcId()) != null && BSOriginalNpcData.getEvent(original.getNpcId()).mapId() == 37)).isEmpty();
    }

    private record SourceActors(ServerLevel level, List<EntityOriginalNpc> actors) {
    }

    private static boolean valid(int x, int y) {
        return x >= 0 && x <= 64 && y >= 34 && y <= 64;
    }

    private static JsonObject loadSource() {
        try (var stream = OriginalPoolCrowdMovementService.class.getResourceAsStream("/data/blacksouls/original_symbol_map037.json")) {
            if (stream == null) throw new IllegalStateException("Missing original pool crowd passage");
            var data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (data.get("version").getAsInt() != 1 || data.get("mapId").getAsInt() != 37
                    || data.get("minX").getAsInt() != 0 || data.get("maxX").getAsInt() != 64
                    || data.get("minY").getAsInt() != 34 || data.get("maxY").getAsInt() != 64
                    || Base64.getDecoder().decode(data.get("basePassageMasks").getAsString()).length != 2015) throw new IllegalArgumentException("Invalid original pool crowd passage");
            return data;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
