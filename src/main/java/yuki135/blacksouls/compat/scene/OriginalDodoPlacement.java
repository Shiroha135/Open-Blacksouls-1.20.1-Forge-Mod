package yuki135.blacksouls.compat.scene;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class OriginalDodoPlacement {
    public static final String NPC_ID = "map037_ev053";
    public static final Vec3 POSITION = new Vec3(841.5D, 12.0625D, -594.5D);
    private static final List<OriginalNpcPlacements.Site> SITES = List.of(
            new OriginalNpcPlacements.Site(NPC_ID, POSITION, 180.0F, "minecraft:moss_carpet", List.of(
                    landmark(841, 11, -595, "minecraft:cobblestone", Map.of()),
                    landmark(840, 11, -596, "minecraft:stone_brick_stairs", Map.of("facing", "south", "half", "bottom", "shape", "straight")),
                    landmark(841, 11, -596, "minecraft:stone_brick_stairs", Map.of("facing", "south", "half", "bottom", "shape", "straight")),
                    landmark(842, 11, -596, "minecraft:stone_brick_stairs", Map.of("facing", "south", "half", "bottom", "shape", "straight")),
                    landmark(855, 11, -605, "mcwtrpdoors:jungle_cottage_trapdoor", Map.of("facing", "south", "half", "bottom", "open", "false"))), true));

    private OriginalDodoPlacement() {
    }

    public static List<OriginalNpcPlacements.Site> sites() {
        return SITES;
    }

    private static OriginalNpcPlacements.Landmark landmark(int x, int y, int z, String id, Map<String, String> properties) {
        return new OriginalNpcPlacements.Landmark(new BlockPos(x, y, z), id, properties);
    }
}
