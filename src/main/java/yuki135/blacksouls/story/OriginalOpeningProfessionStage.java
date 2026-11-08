package yuki135.blacksouls.story;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import yuki135.blacksouls.handler.OriginalOpeningVisualBlocks;
import static yuki135.blacksouls.story.OriginalOpeningScenes.*;

public final class OriginalOpeningProfessionStage {
    public static final int MAX_BLOCKS = 2048;
    public static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("blacksouls", "opening/profession");

    private OriginalOpeningProfessionStage() {
    }

    public record Template(Map<BlockPos, Material> blocks) {
        public Template {
            if (blocks == null || blocks.isEmpty() || blocks.size() > MAX_BLOCKS) throw new IllegalArgumentException("出身布景必须包含 1 至 " + MAX_BLOCKS + " 个非空气方块。");
            var copy = new LinkedHashMap<BlockPos, Material>();
            for (var entry : blocks.entrySet()) {
                var position = entry.getKey(); var material = entry.getValue();
                if (position == null || material == null || position.getX() < 0 || position.getX() >= SceneKind.OP.width()
                        || position.getZ() < 0 || position.getZ() >= SceneKind.OP.height()
                        || position.getY() < MIN_Y - FEET_Y || position.getY() >= MAX_Y - FEET_Y
                        || material.state().hasBlockEntity() || !material.state().getFluidState().isEmpty()) {
                    throw new IllegalArgumentException("出身布景超出边界，或包含方块实体、液体。");
                }
                copy.put(position.immutable(), material);
            }
            blocks = Collections.unmodifiableMap(copy);
        }

        public CompoundTag save() {
            var result = new CompoundTag(); var palette = new ListTag(); var indices = new LinkedHashMap<Material, Integer>();
            var cells = new ArrayList<Integer>();
            for (var entry : blocks.entrySet()) {
                int paletteIndex = indices.computeIfAbsent(entry.getValue(), material -> {
                    var row = new CompoundTag(); row.put("State", NbtUtils.writeBlockState(material.state())); row.putBoolean("Solid", material.solid());
                    palette.add(row); return palette.size() - 1;
                });
                var position = entry.getKey();
                cells.add((position.getY() + FEET_Y - MIN_Y) * 400 + position.getZ() * 20 + position.getX()); cells.add(paletteIndex);
            }
            result.put("Palette", palette); result.putIntArray("Blocks", cells); return result;
        }

        public static Template load(CompoundTag tag) {
            if (!tag.getAllKeys().equals(Set.of("Palette", "Blocks")) || !tag.contains("Palette", Tag.TAG_LIST) || !tag.contains("Blocks", Tag.TAG_INT_ARRAY)) {
                throw new IllegalArgumentException("Invalid profession template storage schema");
            }
            var palette = tag.getList("Palette", Tag.TAG_COMPOUND); int[] cells = tag.getIntArray("Blocks");
            if (palette.isEmpty() || palette.size() > MAX_BLOCKS || cells.length == 0 || cells.length > MAX_BLOCKS * 2 || cells.length % 2 != 0) {
                throw new IllegalArgumentException("Invalid profession template storage size");
            }
            var materials = new ArrayList<Material>();
            for (var value : palette) {
                var row = (CompoundTag) value;
                if (!row.getAllKeys().equals(Set.of("State", "Solid")) || !row.contains("State", Tag.TAG_COMPOUND)
                        || !row.contains("Solid", Tag.TAG_BYTE) || row.getByte("Solid") < 0 || row.getByte("Solid") > 1) {
                    throw new IllegalArgumentException("Invalid profession template palette");
                }
                var stateTag = row.getCompound("State");
                var state = BlockState.CODEC.parse(NbtOps.INSTANCE,stateTag).result().orElseThrow(() -> new IllegalArgumentException("Missing profession template block state"));
                if (state.isAir() || !NbtUtils.writeBlockState(state).equals(stateTag)) throw new IllegalArgumentException("Missing or changed profession template block state");
                materials.add(Material.of(state, row.getBoolean("Solid")));
            }
            var result = new LinkedHashMap<BlockPos, Material>();
            for (int i = 0; i < cells.length; i += 2) {
                int cell = cells[i], index = cells[i + 1];
                if (cell < 0 || cell >= 400 * (MAX_Y - MIN_Y) || index < 0 || index >= materials.size()) throw new IllegalArgumentException("Invalid profession template cell");
                var position = new BlockPos(cell % 20, cell / 400 + MIN_Y - FEET_Y, cell % 400 / 20);
                if (result.put(position, materials.get(index)) != null) throw new IllegalArgumentException("Duplicate profession template cell");
            }
            return new Template(result);
        }
    }

    public static Template defaultTemplate() {
        return OriginalOpeningProfessionLayout.template();
    }

    public static Plan plan(OriginalOpeningSceneData.Lease lease, Template template) {
        var result = new LinkedHashMap<BlockPos, Material>(); var start = origin(lease, SceneKind.OP);
        template.blocks().forEach((position, material) -> result.put(start.offset(position), material));
        return new Plan(lease, SceneKind.OP, Set.of(), result);
    }

    public static AABB bounds(BlockPos origin) {
        return new AABB(origin.offset(0, MIN_Y - FEET_Y, 0), origin.offset(20, MAX_Y - FEET_Y, 20));
    }

    public static void requireRegion(ServerLevel level, BlockPos origin) {
        var bounds = bounds(origin);
        if (bounds.minY < level.getMinBuildHeight() || bounds.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(bounds)) throw new IllegalArgumentException("布景范围超出世界边界或建造高度。");
        for (int x=origin.getX()>>4;x<=(origin.getX()+19)>>4;x++) for (int z=origin.getZ()>>4;z<=(origin.getZ()+19)>>4;z++)
            if (level.getChunkSource().getChunkNow(x,z)==null || !level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(x,z).toLong()))
                throw new IllegalArgumentException("请先靠近布景，让整个 20×20 区域加载完成。");
    }

    public static int create(ServerLevel level, BlockPos origin, Template template) {
        requireRegion(level, origin); var bounds = bounds(origin);
        for (var position : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ), BlockPos.containing(bounds.maxX - 1, bounds.maxY - 1, bounds.maxZ - 1))) {
            if (!level.getBlockState(position).isAir() || level.getBlockEntity(position) != null) throw new IllegalArgumentException("布景区域必须全部为空气，未覆盖已有方块：" + position.toShortString());
        }
        var target = new LinkedHashMap<BlockPos, Material>();
        template.blocks().forEach((position, material) -> target.put(origin.offset(position), material));
        return place(level, target);
    }

    public static int piece(ServerLevel level, String module, BlockPos origin, Direction facing) {
        var blocks = new LinkedHashMap<BlockPos, Material>();
        if(module.startsWith("source_"))yuki135.blacksouls.handler.OriginalOpeningSourceBlocks.append(blocks,module,origin,facing);
        else module(blocks, module, origin, facing);
        return place(level, blocks);
    }

    private static int place(ServerLevel level, Map<BlockPos, Material> blocks) {
        for (var entry : blocks.entrySet()) {
            var position = entry.getKey();
            if (position.getY() < level.getMinBuildHeight() || position.getY() >= level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(position)
                    || level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4) == null
                    || !level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(position).toLong())) throw new IllegalArgumentException("布景部件超出已加载的可建造区域。");
            if (!level.getBlockState(position).isAir() || level.getBlockEntity(position) != null) throw new IllegalArgumentException("未覆盖已有方块：" + position.toShortString());
            if (entry.getValue().solid() && !level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(position), entity -> !entity.isSpectator() && !entity.isRemoved()).isEmpty()) {
                throw new IllegalArgumentException("请让实体离开待放置的布景方块。");
            }
        }
        var written = new ArrayList<Map.Entry<BlockPos, Material>>();
        for (var entry : blocks.entrySet()) {
            var position = entry.getKey(); var state = entry.getValue().state();
            if (!level.setBlock(position, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE) || !level.getBlockState(position).equals(state)) {
                for (var previous : written) level.setBlock(previous.getKey(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                throw new IllegalStateException("布景放置失败，已撤回本次放置的方块。");
            }
            written.add(entry);
        }
        return written.size();
    }

    public static Template capture(ServerLevel level, BlockPos origin) {
        requireRegion(level, origin); var blocks = new LinkedHashMap<BlockPos, Material>(); var bounds = bounds(origin);
        for (var position : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ), BlockPos.containing(bounds.maxX - 1, bounds.maxY - 1, bounds.maxZ - 1))) {
            var state = level.getBlockState(position);
            if (state.isAir()) continue;
            if (level.getBlockEntity(position) != null || state.hasBlockEntity() || !state.getFluidState().isEmpty()) throw new IllegalArgumentException("布景暂不支持容器、告示牌等方块实体或液体：" + position.toShortString());
            blocks.put(position.subtract(origin).immutable(), Material.of(state, !state.getCollisionShape(level, position).isEmpty()));
            if (blocks.size() > MAX_BLOCKS) throw new IllegalArgumentException("出身布景最多支持 " + MAX_BLOCKS + " 个非空气方块。");
        }
        var template = new Template(blocks);
        var lease = new OriginalOpeningSceneData.Lease(new java.util.UUID(0, 0), 0, template);
        var cells = new ArrayList<Cell>(); cells.add(new Cell(10, 9));
        professionAnchors(lease).forEach(anchor -> cells.add(anchor.sourceCell()));
        for (var cell : cells) {
            var feet = origin.offset(cell.x(), 0, cell.y()); var floor = feet.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                    || !level.noCollision(new AABB(feet.getX() + .2D, feet.getY(), feet.getZ() + .2D, feet.getX() + .8D, feet.getY() + 1.8D, feet.getZ() + .8D))) {
                throw new IllegalArgumentException("出生点或职业位置需要完整地面与净空：" + feet.toShortString());
            }
            try { checkpoint(plan(lease, template), cell); }
            catch (IllegalArgumentException failure) { throw new IllegalArgumentException("出生点或职业位置上方需要 4 格净空：" + feet.toShortString()); }
        }
        return template;
    }

    private static void module(Map<BlockPos, Material> blocks, String id, BlockPos origin) {
        module(blocks,id,origin,Direction.NORTH);
    }

    private static void module(Map<BlockPos, Material> blocks, String id, BlockPos origin, Direction facing) {
        var definition=OriginalOpeningVisualBlocks.definition(id);
        for (int index=0;index<definition.parts().size();index++) {
            var part=definition.parts().get(index); var position=origin.offset(part.offset(facing));
            var material=Material.of(OriginalOpeningVisualBlocks.state(id,index,facing),part.hasCollision());
            if (blocks.putIfAbsent(position,material)!=null) throw new IllegalStateException("Profession stage visual blocks overlap at "+position);
        }
    }
}
