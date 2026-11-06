package yuki135.blacksouls.compat.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;

public final class OriginalBillPlacement {
    public static final String INITIAL_NPC = "map062_ev076";
    public static final String PROXY_NPC = "map062_ev002";
    public static final String REGULAR_NPC = "map062_ev084";
    public static final BlockPos LADDER = new BlockPos(462, -37, -472);
    public static final Vec3 INITIAL_POSITION = new Vec3(462.5D, -37.5D, -471.5D);
    public static final Vec3 PROXY_POSITION = new Vec3(462.5D, -37.5D, -472.5D);
    public static final Vec3 REGULAR_POSITION = new Vec3(466.5D, -37.0D, -477.5D);

    private OriginalBillPlacement() {
    }

    public static List<OriginalNpcPlacements.Site> sites() {
        return List.of(
                new OriginalNpcPlacements.Site(INITIAL_NPC, INITIAL_POSITION, 0.0F,
                        "minecraft:spruce_slab", initialLandmarks(), true),
                new OriginalNpcPlacements.Site(PROXY_NPC, PROXY_POSITION, 180.0F,
                        "minecraft:spruce_slab", proxyLandmarks(), true),
                new OriginalNpcPlacements.Site(REGULAR_NPC, REGULAR_POSITION, 180.0F,
                        "minecraft:grass_block", roomLandmarks()));
    }

    public static boolean allowsFootWater(ServerLevel level, OriginalNpcPlacements.Site site, AABB bounds) {
        if (!sites().contains(site) || !site.npcId().equals(INITIAL_NPC) && !site.npcId().equals(PROXY_NPC)) return false;
        BlockPos support = BlockPos.containing(site.position().add(0.0D, -0.02D, 0.0D));
        var state = level.getBlockState(support);
        return state.is(net.minecraft.world.level.block.Blocks.SPRUCE_SLAB)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                && state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE) == net.minecraft.world.level.block.state.properties.SlabType.BOTTOM
                && !level.containsAnyLiquid(new AABB(bounds.minX, bounds.minY + 0.5D, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ));
    }

    public static int playerDirection(net.minecraft.world.entity.Entity player) {
        return switch (player.getDirection()) {
            case SOUTH -> 8;
            case NORTH -> 2;
            case WEST -> 6;
            case EAST -> 4;
            default -> throw new UnsupportedOperationException("bill_character_direction");
        };
    }

    private static List<OriginalNpcPlacements.Landmark> roomLandmarks() {
        return List.of(
                landmark(472, -37, -476, "decoration_mod:diorite_brick_column_down", Map.of()),
                landmark(460, -37, -476, "decoration_mod:diorite_brick_column_down", Map.of()),
                landmark(472, -37, -483, "decoration_mod:diorite_brick_column_down", Map.of()),
                landmark(460, -37, -483, "decoration_mod:diorite_brick_column_down", Map.of()),
                landmark(462, -37, -472, "blacksouls2:t_tie_ti_zi", Map.of("facing", "north")));
    }

    private static List<OriginalNpcPlacements.Landmark> initialLandmarks() {
        var landmarks = new java.util.ArrayList<>(roomLandmarks());
        landmarks.add(landmark(462, -38, -472, "minecraft:spruce_slab", Map.of("type", "bottom", "waterlogged", "true")));
        landmarks.add(landmark(462, -36, -472, "blacksouls2:t_tie_ti_zi", Map.of("facing", "north")));
        return List.copyOf(landmarks);
    }

    private static List<OriginalNpcPlacements.Landmark> proxyLandmarks() {
        var landmarks = new java.util.ArrayList<>(roomLandmarks());
        landmarks.add(landmark(462, -38, -473, "minecraft:spruce_slab", Map.of("type", "bottom", "waterlogged", "true")));
        return List.copyOf(landmarks);
    }

    private static OriginalNpcPlacements.Landmark landmark(int x, int y, int z, String blockId,
                                                         Map<String, String> properties) {
        return new OriginalNpcPlacements.Landmark(new BlockPos(x, y, z), blockId, properties);
    }
}
