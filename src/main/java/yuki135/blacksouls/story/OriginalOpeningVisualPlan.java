package yuki135.blacksouls.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import static yuki135.blacksouls.story.OriginalOpeningScenes.*;

public final class OriginalOpeningVisualPlan {
    private static final byte[] CATALOG_BYTES=resource("original_opening_visual_blocks.json");
    private static final byte[] ASSEMBLY_BYTES=resource("original_opening_visual_assembly.json");
    private static final JsonObject ASSEMBLY=object(ASSEMBLY_BYTES);
    private static final Map<String,Module> MODULES=modules(object(CATALOG_BYTES));
    private static final List<Instance> INSTANCES=instances(ASSEMBLY);
    private static final String FLOWER_GROUND=ground("sourceA2kind24");
    private static final String PLAIN_GROUND=ground("sourceA2kind32");
    private static final String STONE_GROUND=groundId("original_opening_stone");
    private static final Map<StateKey,Material> RESOLVED=new java.util.concurrent.ConcurrentHashMap<>();

    private OriginalOpeningVisualPlan() {
    }

    public static void verifyInputs(String catalog,String assembly,String golden) {
        if (!hash(CATALOG_BYTES).equals(catalog) || !hash(ASSEMBLY_BYTES).equals(assembly)
                || !golden.matches("[a-f0-9]{64}") || !golden.equals(ASSEMBLY.get("sourceGoldenSha256").getAsString())
                || !ASSEMBLY.get("sourceTopologyUnchanged").getAsBoolean()
                || !ASSEMBLY.get("bakedChunksMergeOverlappingAuthoringModulesBeforePlacement").getAsBoolean()) {
            throw new IllegalArgumentException("Opening visual inputs disagree with the canonical source catalog");
        }
    }

    public static Plan plan(OriginalOpeningSceneData.Lease lease,SceneKind kind,Set<Cell> temporary) {
        requireTemporary(kind,temporary);
        if (!kind.island()) throw new IllegalArgumentException("Opening visual plan requires an island source phase");
        var result=new LinkedHashMap<BlockPos,Material>();var start=origin(lease,kind);
        for (int y=0;y<kind.height();y++) for (int x=0;x<kind.width();x++) {
            var cell=new Cell(x,y);var floor=new BlockPos(start.getX()+x,FEET_Y-1,start.getZ()+y);
            if (sourceSupport(kind,cell)) {
                int tile=sourceFloorTile(kind,cell);
                String id=tile>=2048 && (tile-2048)/48==32?PLAIN_GROUND:FLOWER_GROUND;
                put(result,floor,material(MODULES.get(id).parts().get(0),Direction.NORTH));
                if (interior(kind,cell)) put(result,floor.below(),material(MODULES.get(STONE_GROUND).parts().get(0),Direction.NORTH));
            } else if (temporary.contains(cell)) put(result,floor,Material.TEMPORARY_SUPPORT);
        }
        for (var instance:INSTANCES) {
            if (instance.support()!=null && !sourceSupport(kind,instance.support())) continue;
            for (var part:instance.module().parts()) {
                var offset=rotate(part.offset(),instance.facing());
                var position=start.offset(instance.anchor().getX()+offset.getX(),instance.anchor().getY()+offset.getY(),instance.anchor().getZ()+offset.getZ());
                put(result,position,material(part,instance.facing()));
            }
        }
        return new Plan(lease,kind,temporary,result);
    }

    public static int moduleCount() { return MODULES.size(); }
    public static int instanceCount() { return INSTANCES.size(); }
    public static int treePartCount() { return INSTANCES.stream().filter(instance->instance.support()==null).mapToInt(instance->instance.module().parts().size()).sum(); }

    private static boolean interior(SceneKind kind,Cell cell) {
        for (var next:List.of(new Cell(cell.x()-1,cell.y()),new Cell(cell.x()+1,cell.y()),new Cell(cell.x(),cell.y()-1),new Cell(cell.x(),cell.y()+1))) {
            if (!validCell(kind,next) || !sourceSupport(kind,next)) return false;
        }
        return true;
    }

    private static void put(Map<BlockPos,Material> result,BlockPos position,Material material) {
        if (result.putIfAbsent(position.immutable(),material)!=null) throw new IllegalArgumentException("Opening visual instances overlap at "+position);
    }

    private static Material material(Part part,Direction facing) {
        return RESOLVED.computeIfAbsent(new StateKey(part.id(),facing),key->resolve(part,facing));
    }

    private static Material resolve(Part part,Direction facing) {
        var id=ResourceLocation.fromNamespaceAndPath("blacksouls",part.id());
        if (!ForgeRegistries.BLOCKS.containsKey(id)) throw new IllegalStateException("Opening visual block was not registered: "+id);
        var block=ForgeRegistries.BLOCKS.getValue(id);
        if (block==null || block==Blocks.AIR || !id.equals(ForgeRegistries.BLOCKS.getKey(block))) throw new IllegalStateException("Opening visual registry resolved another block: "+id);
        BlockState state=block.defaultBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) throw new IllegalStateException("Opening visual block lacks its model facing: "+id);
        return Material.of(state.setValue(BlockStateProperties.HORIZONTAL_FACING,facing),part.collision());
    }

    private static String ground(String key) {
        return groundId(ASSEMBLY.getAsJsonObject("groundPalette").get(key).getAsString());
    }

    private static String groundId(String id) {
        var module=MODULES.get(id);
        if (module==null || !module.category().equals("ground") || module.parts().size()!=1 || !module.parts().get(0).collision()
                || !module.parts().get(0).offset().equals(BlockPos.ZERO)) throw new IllegalArgumentException("Invalid opening visual ground palette");
        return id;
    }

    private record Part(String id,BlockPos offset,boolean collision) { }
    private record StateKey(String id,Direction facing) { }
    private record Module(String id,String category,List<Part> parts) { }
    private record Instance(Module module,BlockPos anchor,Direction facing,Cell support) { }

    private static Map<String,Module> modules(JsonObject catalog) {
        if (!catalog.keySet().equals(Set.of("version","region","blocks")) || integer(catalog,"version")!=1) throw new IllegalArgumentException("Invalid opening visual catalog schema");
        var result=new LinkedHashMap<String,Module>();var ids=new LinkedHashSet<String>();
        for (var value:catalog.getAsJsonArray("blocks")) {
            var row=value.getAsJsonObject();String id=row.get("id").getAsString(),category=row.get("category").getAsString();
            if (!id.matches("original_opening_[a-z0-9_]+") || !Set.of("tree","baked_tree","ground","flora").contains(category)) throw new IllegalArgumentException("Invalid opening visual module");
            var parts=new ArrayList<Part>();var offsets=new LinkedHashSet<BlockPos>();
            var values=row.getAsJsonArray("parts");
            if (values.isEmpty() || values.size()>64) throw new IllegalArgumentException("Opening visual part count outside bound");
            for (int index=0;index<values.size();index++) {
                var part=values.get(index).getAsJsonObject();
                var offset=position(part.getAsJsonArray("offset"));
                if (offset.getX()<0 || offset.getX()>3 || offset.getY()<0 || offset.getY()>4 || offset.getZ()<0 || offset.getZ()>3
                        || index==0 && !offset.equals(BlockPos.ZERO) || !offsets.add(offset)) throw new IllegalArgumentException("Invalid opening visual part offset");
                var boxes=part.getAsJsonArray("collision");
                if (boxes.size()>256) throw new IllegalArgumentException("Opening visual collision count outside bound");
                for (var box:boxes) {
                    var coordinates=box.getAsJsonArray();
                    if (coordinates.size()!=6) throw new IllegalArgumentException("Invalid opening visual collision bounds");
                    for (int axis=0;axis<3;axis++) {
                        double low=coordinates.get(axis).getAsDouble(),high=coordinates.get(axis+3).getAsDouble();
                        if (!Double.isFinite(low) || !Double.isFinite(high) || low<0 || high>16 || low>=high) throw new IllegalArgumentException("Opening visual collision escaped its block");
                    }
                }
                String partId=index==0?id:id+"_part_"+index;
                if (!ids.add(partId)) throw new IllegalArgumentException("Duplicate opening visual block id");
                parts.add(new Part(partId,offset.immutable(),!boxes.isEmpty()));
            }
            if (result.put(id,new Module(id,category,List.copyOf(parts)))!=null) throw new IllegalArgumentException("Duplicate opening visual module");
        }
        if (result.isEmpty() || result.size()>64 || ids.size()>1024) throw new IllegalArgumentException("Opening visual catalog exceeds bound");
        return Map.copyOf(result);
    }

    private static List<Instance> instances(JsonObject assembly) {
        if (integer(assembly,"version")!=1) throw new IllegalArgumentException("Invalid opening visual assembly version");
        var result=new ArrayList<Instance>();
        for (var value:assembly.getAsJsonArray("instances")) {
            var row=value.getAsJsonObject();String name=row.get("module").getAsString();
            var module=MODULES.get("original_opening_"+name);
            var anchor=position(row.getAsJsonArray("anchor"));
            String facingName=row.get("facing").getAsString();
            Direction facing=switch(facingName) {case "north"->Direction.NORTH;case "east"->Direction.EAST;case "south"->Direction.SOUTH;case "west"->Direction.WEST;default->throw new IllegalArgumentException("Opening visual facing is not horizontal");};
            if (module==null || anchor.getX()<0 || anchor.getX()>=30 || anchor.getY()<0 || anchor.getY()>10 || anchor.getZ()<0 || anchor.getZ()>=24) throw new IllegalArgumentException("Invalid opening visual instance");
            Cell support=null;
            if (row.has("requiresSupportCell")) {
                var pair=row.getAsJsonArray("requiresSupportCell");
                if (pair.size()!=2) throw new IllegalArgumentException("Invalid opening flora support cell");
                support=new Cell(number(pair,0),number(pair,1));
                if (!module.category().equals("flora") || support.x()<0 || support.x()>=30 || support.y()<0 || support.y()>=24) throw new IllegalArgumentException("Opening flora support escaped its source grid");
            } else if (!name.startsWith("tree_chunk_") || !module.category().equals("baked_tree")) throw new IllegalArgumentException("Opening assembly may place only baked tree chunks and supported flora");
            if (module.category().equals("flora") && support==null) throw new IllegalArgumentException("Opening flora lacks phase support");
            result.add(new Instance(module,anchor.immutable(),facing,support));
        }
        if (result.isEmpty() || result.size()>256) throw new IllegalArgumentException("Opening visual instance count outside bound");
        return List.copyOf(result);
    }

    private static BlockPos rotate(BlockPos offset,Direction facing) {
        return switch(facing) {case NORTH->offset;case EAST->new BlockPos(-offset.getZ(),offset.getY(),offset.getX());case SOUTH->new BlockPos(-offset.getX(),offset.getY(),-offset.getZ());case WEST->new BlockPos(offset.getZ(),offset.getY(),-offset.getX());default->throw new IllegalArgumentException("Invalid opening visual rotation");};
    }

    private static BlockPos position(JsonArray array) {
        if (array==null || array.size()!=3) throw new IllegalArgumentException("Invalid opening visual position");
        return new BlockPos(number(array,0),number(array,1),number(array,2));
    }

    private static int number(JsonArray array,int index) { return array.get(index).getAsBigDecimal().intValueExact(); }
    private static int integer(JsonObject object,String key) { return object.get(key).getAsBigDecimal().intValueExact(); }
    private static JsonObject object(byte[] bytes) { return JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject(); }
    private static String hash(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch(java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    private static byte[] resource(String name) {
        try (var stream=OriginalOpeningVisualPlan.class.getResourceAsStream("/data/blacksouls/"+name)) {
            if (stream==null) throw new IllegalStateException("Missing opening visual resource: "+name);
            return stream.readAllBytes();
        } catch(java.io.IOException failure) { throw new IllegalStateException("Unable to read opening visual resource: "+name,failure); }
    }
}
