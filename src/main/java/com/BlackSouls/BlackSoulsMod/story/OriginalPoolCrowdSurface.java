package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalNpcPlacements;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalPoolCrowdPlacement;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OriginalPoolCrowdSurface {
    public static final String BLOOD_WATER = "blacksouls2:xie_shui";
    public static final double FEET_Y = 11.0D;
    private static final Set<String> IDENTITIES = Set.of("map037_ev003", "map037_ev004", "map037_ev005", "map037_ev006", "map037_ev007",
            "map037_ev008", "map037_ev009", "map037_ev010", "map037_ev011", "map037_ev054");
    private static final byte[] PASSAGE_MASKS = loadPassage();

    private OriginalPoolCrowdSurface() {
    }

    public static boolean isCrowd(String identity) {
        return identity != null && IDENTITIES.contains(identity);
    }

    public static double planeHeight() {
        return FEET_Y;
    }

    public static boolean isLowerPoolPosition(double x, double z) {
        return Double.isFinite(x) && Double.isFinite(z) && x >= 821.0D && x < 886.0D && z >= -613.0D && z < -582.0D;
    }

    public static boolean matchesSite(ServerLevel level, OriginalNpcPlacements.Site site) {
        if (site == null || !HokoniwaDestination.DIMENSION.equals(level.dimension()) || !isCrowd(site.npcId())
                || !site.equals(OriginalPoolCrowdPlacement.site(site.npcId())) || !OriginalNpcPlacements.hasDeployedMap(level)
                || !OriginalNpcPlacements.chunksReady(level, site)
                || site.landmarks().stream().anyMatch(landmark -> !landmark.matches(level))) return false;
        return matchesColumn(level, BlockPos.containing(site.position()).getX(), BlockPos.containing(site.position()).getZ());
    }

    public static boolean allowsPlacement(ServerLevel level, OriginalNpcPlacements.Site site, AABB bounds) {
        return matchesSite(level, site) && supports(level, site.npcId(), null, site.position(), bounds)
                && level.noCollision(null, bounds);
    }

    public static boolean supports(ServerLevel level, String identity, Vec3 position, AABB bounds) {
        return supports(level, identity, null, position, bounds);
    }

    public static boolean supports(ServerLevel level, EntityOriginalNpc actor, Vec3 position, AABB bounds) {
        return actor != null && actor.level() == level && supports(level, actor.getNpcId(), actor, position, bounds);
    }

    public static boolean supports(ServerLevel level, String identity, Entity actor, Vec3 position, AABB bounds) {
        if (position == null || bounds == null || actor != null && actor.level() != level
                || actor instanceof EntityOriginalNpc npc && !npc.getNpcId().equals(identity)
                || !HokoniwaDestination.DIMENSION.equals(level.dimension()) || !isCrowd(identity)
                || !isLowerPoolPosition(position.x, position.z) || !Double.isFinite(position.y)
                || Math.abs(position.y - FEET_Y) > 1.0E-7D || Math.abs(bounds.minY - position.y) > 1.0E-7D
                || !Double.isFinite(bounds.minX) || !Double.isFinite(bounds.minZ) || !Double.isFinite(bounds.maxX)
                || !Double.isFinite(bounds.maxY) || !Double.isFinite(bounds.maxZ)
                || bounds.getXsize() <= 0.0D || bounds.getXsize() > 1.0D || bounds.getZsize() <= 0.0D || bounds.getZsize() > 1.0D
                || bounds.getYsize() <= 0.0D || bounds.getYsize() > 3.0D) return false;
        int minX = BlockPos.containing(bounds.minX + 1.0E-7D, 0, 0).getX();
        int maxX = BlockPos.containing(bounds.maxX - 1.0E-7D, 0, 0).getX();
        int minZ = BlockPos.containing(0, 0, bounds.minZ + 1.0E-7D).getZ();
        int maxZ = BlockPos.containing(0, 0, bounds.maxZ - 1.0E-7D).getZ();
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (!matchesColumn(level, x, z)) return false;
        }
        boolean clear = actor != null ? level.noCollision(actor, bounds) : clearBlocks(level, bounds);
        return clear && !level.containsAnyLiquid(new AABB(bounds.minX, FEET_Y + 0.02D,
                bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ));
    }

    private static boolean clearBlocks(ServerLevel level, AABB bounds) {
        for (var shape : level.getBlockCollisions(null, bounds)) if (!shape.isEmpty()) return false;
        return true;
    }

    private static boolean matchesColumn(ServerLevel level, int x, int z) {
        if (!isLowerPoolPosition(x + 0.5D, z + 0.5D)) return false;
        int sourceX = 885 - x, sourceY = -549 - z;
        if ((PASSAGE_MASKS[(sourceY - 34) * 65 + sourceX] & 15) == 0) return false;
        BlockPos water = new BlockPos(x, 10, z);
        BlockPos bottom = water.below();
        if (!level.hasChunkAt(bottom) || !level.hasChunkAt(water.above(4))) return false;
        var state = level.getBlockState(water);
        var fluid = state.getFluidState();
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(BLOOD_WATER)
                && !fluid.isEmpty() && fluid.isSource() && level.getBlockState(bottom).is(Blocks.COARSE_DIRT)
                && level.getBlockState(bottom).isFaceSturdy(level, bottom, net.minecraft.core.Direction.UP);
    }

    private static byte[] loadPassage() {
        try (var stream = OriginalPoolCrowdSurface.class.getResourceAsStream("/data/blacksouls/original_symbol_map037.json")) {
            if (stream == null) throw new IllegalStateException("Missing original lower pool passage");
            var data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            byte[] masks = Base64.getDecoder().decode(data.get("basePassageMasks").getAsString());
            if (data.get("version").getAsInt() != 1 || data.get("mapId").getAsInt() != 37
                    || data.get("minX").getAsInt() != 0 || data.get("maxX").getAsInt() != 64
                    || data.get("minY").getAsInt() != 34 || data.get("maxY").getAsInt() != 64 || masks.length != 2015) {
                throw new IllegalArgumentException("Invalid original lower pool passage");
            }
            for (byte mask : masks) if (mask < 0 || mask > 15) throw new IllegalArgumentException("Invalid original lower pool direction mask");
            return masks;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
