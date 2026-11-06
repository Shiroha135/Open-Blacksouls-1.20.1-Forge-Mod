package com.BlackSouls.BlackSoulsMod.compat.scene;

import com.BlackSouls.BlackSoulsMod.story.OriginalPoolCrowdSurface;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class OriginalPoolCrowdPlacement {
    private static final List<OriginalNpcPlacements.Site> SITES = List.of(
            site(4, 844, -597, 90.0F),
            site(3, 844, -596, 90.0F),
            site(5, 841, -598, 90.0F),
            site(6, 838, -598, 0.0F),
            site(7, 838, -595, 0.0F),
            site(8, 842, -593, 270.0F),
            site(9, 839, -593, 270.0F),
            site(10, 843, -593, 270.0F),
            site(11, 844, -594, 180.0F),
            site(54, 842, -598, 90.0F));

    private OriginalPoolCrowdPlacement() {
    }

    public static List<OriginalNpcPlacements.Site> sites() {
        return SITES;
    }

    public static OriginalNpcPlacements.Site site(String identity) {
        return SITES.stream().filter(site -> site.npcId().equals(identity)).findFirst().orElse(null);
    }

    private static OriginalNpcPlacements.Site site(int event, int x, int z, float yaw) {
        return new OriginalNpcPlacements.Site("map037_ev%03d".formatted(event),
                new Vec3(x + 0.5D, OriginalPoolCrowdSurface.FEET_Y, z + 0.5D), yaw,
                OriginalPoolCrowdSurface.BLOOD_WATER, List.of(
                landmark(841, 11, -595, "minecraft:cobblestone", Map.of()),
                landmark(841, 12, -595, "minecraft:moss_carpet", Map.of()),
                landmark(840, 11, -596, "minecraft:stone_brick_stairs", Map.of("facing", "south", "half", "bottom", "shape", "straight")),
                landmark(842, 11, -596, "minecraft:stone_brick_stairs", Map.of("facing", "south", "half", "bottom", "shape", "straight"))), true);
    }

    private static OriginalNpcPlacements.Landmark landmark(int x, int y, int z, String identity, Map<String, String> properties) {
        return new OriginalNpcPlacements.Landmark(new BlockPos(x, y, z), identity, properties);
    }
}
