package yuki135.blacksouls.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OriginalOpeningScenes {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("blacksouls", "opening"));
    public static final int FORMAT = 2;
    public static final int STRIDE = 1024;
    public static final int COLUMNS = 128;
    public static final int MAX_SLOTS = 16384;
    public static final int FEET_Y = 64;
    public static final int MIN_Y = 60;
    public static final int MAX_Y = 78;
    public static final int MAX_TEMPORARY_SUPPORTS = 4;
    public static final int MAX_PLAN_BLOCKS = 4096;
    private static final Map<SceneKind, SourceMap> MAPS = new EnumMap<>(SceneKind.class);
    private static final List<Role> ROLES;
    public static final String CATALOG_HASH;

    static {
        try (var stream = OriginalOpeningScenes.class.getResourceAsStream("/data/blacksouls/original_opening_scenes.json")) {
            if (stream == null) throw new IllegalStateException("Missing opening scene source data");
            byte[] bytes = stream.readAllBytes();
            CATALOG_HASH = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            var root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.keySet().equals(Set.of("version", "sourceGoldenSha256", "maps", "roles", "alice", "visualCatalogSha256", "visualAssemblySha256"))
                    || root.get("version").getAsInt() != FORMAT || !root.get("sourceGoldenSha256").getAsString().matches("[a-f0-9]{64}")) {
                throw new IllegalArgumentException("Invalid opening source schema");
            }
            OriginalOpeningVisualPlan.verifyInputs(root.get("visualCatalogSha256").getAsString(),
                    root.get("visualAssemblySha256").getAsString(),root.get("sourceGoldenSha256").getAsString());
            for (var value : root.getAsJsonArray("maps")) {
                var row = value.getAsJsonObject(); SceneKind kind = SceneKind.fromMap(row.get("map").getAsInt());
                SourceMap data = new SourceMap(kind, row);
                if (MAPS.put(kind, data) != null) throw new IllegalArgumentException("Duplicate opening source scene");
            }
            if (MAPS.size() != SceneKind.values().length) throw new IllegalArgumentException("Missing opening source scene");
            var roles = new ArrayList<Role>();
            for (var value : root.getAsJsonArray("roles")) {
                var row = value.getAsJsonObject(); var graphic = row.getAsJsonObject("graphic");
                int event = row.get("event").getAsInt(); String id = row.get("sourceId").getAsString();
                if (event != roles.size() + 3 || !id.equals("map007_ev%03d".formatted(event))) throw new IllegalArgumentException("Invalid source role identity");
                Cell cell = new Cell(row.get("x").getAsInt(), row.get("y").getAsInt()); requireCell(SceneKind.OP, cell);
                roles.add(new Role(id, row.get("label").getAsString(), cell,
                        new Graphic(graphic.get("character_name").getAsString(), graphic.get("character_index").getAsInt(),
                                graphic.get("direction").getAsInt(), graphic.get("pattern").getAsInt())));
            }
            if (roles.size() != 9 || !root.getAsJsonObject("alice").get("sourceId").getAsString().equals("map110_ev006")
                    || root.getAsJsonObject("alice").get("x").getAsInt() != 16 || root.getAsJsonObject("alice").get("y").getAsInt() != 12) {
                throw new IllegalArgumentException("Invalid opening actor anchors");
            }
            ROLES = List.copyOf(roles);
        } catch (Exception exception) { throw new IllegalStateException("Failed to load opening scene source data", exception); }
    }

    private OriginalOpeningScenes() {
    }

    public enum SceneKind {
        OP(7,20,20), ISLAND_110(110,30,24), ISLAND_97(97,30,24), ISLAND_98(98,30,24), ISLAND_99(99,30,24);
        private final int map, width, height;
        SceneKind(int map, int width, int height) { this.map = map; this.width = width; this.height = height; }
        public int sourceMap() { return map; }
        public int width() { return width; }
        public int height() { return height; }
        public boolean island() { return this != OP; }
        public static SceneKind fromMap(int map) {
            for (var value : values()) if (value.map == map) return value;
            throw new IllegalArgumentException("Unknown opening scene map: " + map);
        }
    }

    public record Cell(int x, int y) { }
    public record Graphic(String character, int index, int direction, int pattern) {
        public Graphic {
            if (character == null || character.length() > 256 || index < 0 || index > 7
                    || direction != 2 && direction != 4 && direction != 6 && direction != 8 || pattern < 0 || pattern > 2) {
                throw new IllegalArgumentException("Invalid opening source graphic");
            }
        }
    }
    public record Anchor(UUID owner, SceneKind scene, String sourceId, String label, Cell sourceCell, Vec3 feet, Graphic graphic) { }
    private record Role(String id, String label, Cell cell, Graphic graphic) { }
    public record Checkpoint(OriginalOpeningSceneData.Lease lease, SceneKind scene, Cell sourceCell, Vec3 feet, boolean temporarySupport) { }
    public record Landing(Cell requestedSourceCell, boolean temporarySupportRequired, Checkpoint sameCell, Checkpoint stableFallback) { }

    public static final class Material {
        private static final Map<BlockState,Material> INTERN = new java.util.concurrent.ConcurrentHashMap<>();
        public static final Material GRASS=of(Blocks.GRASS_BLOCK.defaultBlockState(),true);
        public static final Material DIRT=of(Blocks.DIRT.defaultBlockState(),true);
        public static final Material STONE=of(Blocks.STONE.defaultBlockState(),true);
        public static final Material PLANKS=of(Blocks.OAK_PLANKS.defaultBlockState(),true);
        public static final Material LOG=of(Blocks.OAK_LOG.defaultBlockState(),true);
        public static final Material LEAVES=of(Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true).setValue(LeavesBlock.DISTANCE,1),true);
        public static final Material FLOWER=of(Blocks.OXEYE_DAISY.defaultBlockState(),false);
        public static final Material TEMPORARY_SUPPORT=of(Blocks.SMOOTH_STONE.defaultBlockState(),true);
        private final BlockState state;
        private final boolean solid;
        private Material(BlockState state,boolean solid) { this.state=state;this.solid=solid; }
        public static Material of(BlockState state,boolean solid) {
            if (state==null || state.isAir()) throw new IllegalArgumentException("Opening owned material cannot be absent");
            var material=INTERN.computeIfAbsent(state,key->new Material(key,solid));
            if (material.solid!=solid) throw new IllegalArgumentException("Opening state has conflicting collision semantics");
            return material;
        }
        public BlockState state() { return state; }
        public boolean solid() { return solid; }
    }

    public record Plan(OriginalOpeningSceneData.Lease lease, SceneKind scene, Set<Cell> temporarySupports, Map<BlockPos,Material> blocks) {
        public Plan {
            if (lease == null || scene == null || blocks == null) throw new IllegalArgumentException("Missing opening plan identity");
            requireTemporary(scene,temporarySupports); temporarySupports = Set.copyOf(temporarySupports);
            var immutable = new LinkedHashMap<BlockPos,Material>();
            for (var entry : blocks.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || !contains(lease,scene,entry.getKey())) throw new IllegalArgumentException("Opening plan escaped leased bounds");
                immutable.put(entry.getKey().immutable(),entry.getValue());
            }
            blocks = Collections.unmodifiableMap(immutable);
            if (blocks.size() > MAX_PLAN_BLOCKS) throw new IllegalArgumentException("Opening scene plan exceeds bound");
        }
        public BlockState state(BlockPos position) { var material = blocks.get(position); return material == null ? Blocks.AIR.defaultBlockState() : material.state(); }
    }

    public static BlockPos origin(OriginalOpeningSceneData.Lease lease, SceneKind kind) {
        if (lease == null || kind == null) throw new IllegalArgumentException("Missing opening scene identity");
        return new BlockPos((lease.slot() % COLUMNS) * STRIDE + (kind == SceneKind.OP ? 64 : 8), FEET_Y,
                (lease.slot() / COLUMNS) * STRIDE + 8);
    }

    public static Vec3 feet(OriginalOpeningSceneData.Lease lease, SceneKind kind, Cell cell) {
        requireCell(kind, cell); var origin = origin(lease, kind);
        return new Vec3(origin.getX() + cell.x + .5D, FEET_Y, origin.getZ() + cell.y + .5D);
    }

    public static AABB bounds(OriginalOpeningSceneData.Lease lease, SceneKind kind) {
        var start = origin(lease, kind);
        return new AABB(start.getX(), MIN_Y, start.getZ(), start.getX() + kind.width, MAX_Y, start.getZ() + kind.height);
    }

    public static boolean contains(OriginalOpeningSceneData.Lease lease, SceneKind kind, BlockPos position) {
        if (position == null) return false; var start = origin(lease, kind);
        return position.getX() >= start.getX() && position.getX() < start.getX() + kind.width
                && position.getZ() >= start.getZ() && position.getZ() < start.getZ() + kind.height
                && position.getY() >= MIN_Y && position.getY() < MAX_Y;
    }

    public static Set<ChunkPos> requiredChunks(OriginalOpeningSceneData.Lease lease, SceneKind kind) {
        var origin = origin(lease, kind); var chunks = new LinkedHashSet<ChunkPos>();
        for (int x = origin.getX() >> 4; x <= (origin.getX() + kind.width - 1) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + kind.height - 1) >> 4; z++) chunks.add(new ChunkPos(x,z));
        }
        return Set.copyOf(chunks);
    }

    public static int regionAt(SceneKind kind, Cell cell) { requireCell(kind,cell); return MAPS.get(kind).regions[index(kind,cell)]; }
    public static int blockedDirections(SceneKind kind, Cell cell) { requireCell(kind,cell); return MAPS.get(kind).blocked[index(kind,cell)]; }
    public static boolean sourceSupport(SceneKind kind, Cell cell) { requireCell(kind,cell); return MAPS.get(kind).support.charAt(index(kind,cell)) == '1'; }
    public static boolean sourcePit(SceneKind kind, Cell cell) { requireCell(kind,cell); return MAPS.get(kind).pit.charAt(index(kind,cell)) == '1'; }
    public static boolean sourceWater(SceneKind kind, Cell cell) { requireCell(kind,cell); return MAPS.get(kind).water.charAt(index(kind,cell)) == '1'; }
    public static int sourceFloorTile(SceneKind kind, Cell cell) { requireCell(kind,cell); return MAPS.get(kind).tiles[index(kind,cell)]; }

    public static Cell sourceCell(OriginalOpeningSceneData.Lease lease, SceneKind kind, Vec3 feet) {
        if (feet == null || !Double.isFinite(feet.x) || !Double.isFinite(feet.y) || !Double.isFinite(feet.z)) throw new IllegalArgumentException("Invalid opening feet position");
        var start = origin(lease,kind); var cell = new Cell((int)Math.floor(feet.x) - start.getX(), (int)Math.floor(feet.z) - start.getZ());
        requireCell(kind,cell); return cell;
    }

    public static boolean canSourceStep(SceneKind kind, Cell from, Cell to) {
        if (!validCell(kind,from) || !validCell(kind,to)) return false;
        int dx = to.x - from.x, dy = to.y - from.y;
        int forward = dx == 1 && dy == 0 ? 4 : dx == -1 && dy == 0 ? 2 : dy == 1 && dx == 0 ? 1 : dy == -1 && dx == 0 ? 8 : 0;
        if (forward == 0) return false; int reverse = forward == 4 ? 2 : forward == 2 ? 4 : forward == 1 ? 8 : 1;
        return (blockedDirections(kind,from) & forward) == 0 && (blockedDirections(kind,to) & reverse) == 0;
    }

    public static boolean enteredRegion10(SceneKind kind, Cell before, Cell after) {
        return validCell(kind,before) && validCell(kind,after) && regionAt(kind,before) != 10 && regionAt(kind,after) == 10;
    }

    public static Plan plan(OriginalOpeningSceneData.Lease lease, SceneKind kind) { return plan(lease,kind,Set.of()); }
    public static Plan plan(OriginalOpeningSceneData.Lease lease, SceneKind kind, Set<Cell> temporary) {
        requireTemporary(kind,temporary);
        if (kind.island()) return OriginalOpeningVisualPlan.plan(lease,kind,temporary);
        var blocks=new LinkedHashMap<BlockPos,Material>();var start=origin(lease,kind);
        for (int y=0;y<kind.height;y++) for (int x=0;x<kind.width;x++) {
            var cell=new Cell(x,y);
            if (sourceSupport(kind,cell)) blocks.put(new BlockPos(start.getX()+x,FEET_Y-1,start.getZ()+y),Material.PLANKS);
        }
        return new Plan(lease,kind,temporary,blocks);
    }

    public static Checkpoint safeSpawn(OriginalOpeningSceneData.Lease lease, SceneKind kind) {
        return checkpoint(plan(lease,kind), kind == SceneKind.OP ? new Cell(10,9) : kind == SceneKind.ISLAND_110 ? new Cell(15,12) : new Cell(15,13));
    }

    public static Checkpoint checkpoint(Plan plan, Cell cell) {
        requireCell(plan.scene,cell); var feet = feet(plan.lease,plan.scene,cell); var floor = BlockPos.containing(feet).below();
        var material = plan.blocks.get(floor);
        if (material == null || !material.solid) throw new IllegalArgumentException("Opening checkpoint has no owned support");
        for (int height = 0; height < 4; height++) {
            var above = plan.blocks.get(BlockPos.containing(feet).above(height));
            if (above != null && above.solid) throw new IllegalArgumentException("Opening checkpoint standing clearance is obstructed");
        }
        return new Checkpoint(plan.lease,plan.scene,cell,feet,plan.temporarySupports.contains(cell));
    }

    public static Landing landing(OriginalOpeningSceneData.Lease lease, SceneKind next, Cell sourceCell) {
        requireCell(next,sourceCell); var fallback = checkpoint(plan(lease,next), next == SceneKind.OP ? new Cell(10,9) : new Cell(15,13));
        if (sourceSupport(next,sourceCell)) return new Landing(sourceCell,false,checkpoint(plan(lease,next),sourceCell),fallback);
        if (sourcePit(next,sourceCell)) return new Landing(sourceCell,true,checkpoint(plan(lease,next,Set.of(sourceCell)),sourceCell),fallback);
        return new Landing(sourceCell,false,null,fallback);
    }

    public static List<Anchor> professionAnchors(OriginalOpeningSceneData.Lease lease) {
        return ROLES.stream().map(role -> new Anchor(lease.owner(),SceneKind.OP,role.id,role.label,role.cell,feet(lease,SceneKind.OP,role.cell),role.graphic)).toList();
    }
    public static Anchor aliceAnchor(OriginalOpeningSceneData.Lease lease, SceneKind kind) {
        if (!kind.island()) throw new IllegalArgumentException("Alice anchor requires island scene"); var cell = new Cell(16,12);
        return new Anchor(lease.owner(),kind,"map110_ev006","Alice",cell,feet(lease,kind,cell),null);
    }

    static void requireTemporary(SceneKind kind, Set<Cell> temporary) {
        if (kind == null || temporary == null || temporary.size() > MAX_TEMPORARY_SUPPORTS || kind == SceneKind.OP && !temporary.isEmpty()) throw new IllegalArgumentException("Invalid temporary opening support set");
        for (var cell : temporary) if (!validCell(kind,cell) || !sourcePit(kind,cell)) throw new IllegalArgumentException("Temporary support requires a source pit");
    }
    static boolean validCell(SceneKind kind, Cell cell) { return kind != null && cell != null && cell.x >= 0 && cell.y >= 0 && cell.x < kind.width && cell.y < kind.height; }
    static void requireCell(SceneKind kind, Cell cell) { if (!validCell(kind,cell)) throw new IllegalArgumentException("Opening source cell outside exact grid"); }
    private static int index(SceneKind kind, Cell cell) { return cell.y * kind.width + cell.x; }
    private static boolean entryClearance(Cell cell) { return cell.x >= 14 && cell.x <= 16 && cell.y >= 11 && cell.y <= 14; }

    private static final class SourceMap {
        final int[] regions, blocked, tiles; final String support, pit, water;
        SourceMap(SceneKind kind, JsonObject row) {
            if (!row.keySet().equals(Set.of("map","width","height","regions","blocked","support","pit","water","floorTiles"))
                    || row.get("width").getAsInt() != kind.width || row.get("height").getAsInt() != kind.height) throw new IllegalArgumentException("Invalid source grid size");
            int cells = kind.width * kind.height;
            regions = integers(row.getAsJsonArray("regions"),cells,255); blocked = integers(row.getAsJsonArray("blocked"),cells,15); tiles = integers(row.getAsJsonArray("floorTiles"),cells,65535);
            support = mask(row,"support",cells); pit = mask(row,"pit",cells); water = mask(row,"water",cells);
            for (int i = 0; i < cells; i++) if ((support.charAt(i)-'0') + (pit.charAt(i)-'0') + (water.charAt(i)-'0') != 1) throw new IllegalArgumentException("Opening physical masks must partition exact source grid");
        }
        private static int[] integers(JsonArray values, int count, int max) {
            if (values == null || values.size() != count) throw new IllegalArgumentException("Invalid source array length"); var result = new int[count];
            for (int i = 0; i < count; i++) { var value = values.get(i); if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Invalid source integer");
                result[i] = value.getAsBigDecimal().intValueExact(); if (result[i] < 0 || result[i] > max) throw new IllegalArgumentException("Source value out of range"); }
            return result;
        }
        private static String mask(JsonObject row, String name, int count) { String value = row.get(name).getAsString(); if (value.length() != count || !value.matches("[01]+")) throw new IllegalArgumentException("Invalid physical source mask"); return value; }
    }
}
