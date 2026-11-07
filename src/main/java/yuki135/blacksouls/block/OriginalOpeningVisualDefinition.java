package yuki135.blacksouls.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OriginalOpeningVisualDefinition {
    public static final String RESOURCE = "/data/blacksouls/original_opening_visual_blocks.json";
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final Set<String> MATERIALS = Set.of("wood","stone","plant");
    private static final Set<String> CATEGORIES = Set.of("tree","baked_tree","ground","flora");
    private final String id;
    private final String material;
    private final String category;
    private final float strength;
    private final int light;
    private final boolean occluding;
    private final List<String> materialIds;
    private final String sourceGeometry;
    private final List<Part> parts;

    public record Catalog(String sha256, List<OriginalOpeningVisualDefinition> definitions) {
        public Catalog { definitions=List.copyOf(definitions); }
    }

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public Box {
            for (double value:new double[]{minX,minY,minZ,maxX,maxY,maxZ}) {
                if (!Double.isFinite(value) || value<0 || value>16) throw invalid("collision range");
            }
            if (minX>=maxX || minY>=maxY || minZ>=maxZ) throw invalid("empty collision box");
        }
    }

    public static final class Part {
        private final BlockPos offset;
        private final List<Box> collision;
        private final VoxelShape[] shapes = new VoxelShape[4];

        private Part(BlockPos offset, List<Box> collision) {
            this.offset=offset.immutable();
            this.collision=List.copyOf(collision);
            for (Direction facing:Direction.Plane.HORIZONTAL) {
                VoxelShape shape=Shapes.empty();
                for (Box box:collision) {
                    Box rotated=rotate(box,facing);
                    shape=Shapes.or(shape,Block.box(rotated.minX(),rotated.minY(),rotated.minZ(),rotated.maxX(),rotated.maxY(),rotated.maxZ()));
                }
                shapes[facing.get2DDataValue()]=shape.optimize();
            }
        }

        public BlockPos offset() { return offset; }
        public BlockPos offset(Direction facing) { return rotate(offset,facing); }
        public List<Box> collision() { return collision; }
        public boolean hasCollision() { return !collision.isEmpty(); }
        public VoxelShape shape(Direction facing) { horizontal(facing);return shapes[facing.get2DDataValue()]; }
    }

    private OriginalOpeningVisualDefinition(JsonObject json) {
        fields(json,Set.of("id","material","strength","light","solid","parts","source","category"));
        id=string(json,"id",100);
        if (!id.matches("original_opening_[a-z0-9_]+") || id.matches(".*_part_[0-9]+")) throw invalid("module id: "+id);
        material=string(json,"material",16);
        category=string(json,"category",16);
        if (!MATERIALS.contains(material) || !CATEGORIES.contains(category)) throw invalid("material or category: "+id);
        double value=number(json.get("strength"));
        if (value<0 || value>50) throw invalid("strength: "+id);
        strength=(float)value;
        light=integer(json.get("light"));
        if (light<0 || light>15) throw invalid("light: "+id);
        occluding=bool(json,"solid");
        JsonObject source=object(json.get("source"));
        fields(source,Set.of("materialIds","geometry"));
        sourceGeometry=string(source,"geometry",256);
        JsonArray sources=array(source.get("materialIds"));
        if (sources.isEmpty() || sources.size()>32) throw invalid("source materials: "+id);
        var materials=new ArrayList<String>();
        var uniqueMaterials=new HashSet<String>();
        for (JsonElement element:sources) {
            String name=string(element,64);
            if (!name.matches("[a-z0-9_]+") || !uniqueMaterials.add(name)) throw invalid("source material id: "+id);
            materials.add(name);
        }
        materialIds=List.copyOf(materials);
        JsonArray sourceParts=array(json.get("parts"));
        if (sourceParts.isEmpty() || sourceParts.size()>64) throw invalid("part count: "+id);
        var offsets=new HashSet<BlockPos>();
        var loaded=new ArrayList<Part>();
        for (JsonElement element:sourceParts) {
            JsonObject row=object(element);
            fields(row,Set.of("offset","collision"));
            JsonArray offset=array(row.get("offset"));
            if (offset.size()!=3) throw invalid("part offset: "+id);
            BlockPos position=new BlockPos(integer(offset.get(0)),integer(offset.get(1)),integer(offset.get(2)));
            if (position.getX()<0 || position.getX()>3 || position.getY()<0 || position.getY()>4 || position.getZ()<0 || position.getZ()>3
                    || !offsets.add(position)) throw invalid("part offset bounds or duplicate: "+id);
            JsonArray boxes=array(row.get("collision"));
            if (boxes.size()>256) throw invalid("part collision count: "+id);
            var collision=new ArrayList<Box>();
            var uniqueBoxes=new HashSet<Box>();
            for (JsonElement boxElement:boxes) {
                JsonArray box=array(boxElement);
                if (box.size()!=6) throw invalid("collision length: "+id);
                Box parsed=new Box(number(box.get(0)),number(box.get(1)),number(box.get(2)),number(box.get(3)),number(box.get(4)),number(box.get(5)));
                if (!uniqueBoxes.add(parsed)) throw invalid("duplicate collision box: "+id);
                collision.add(parsed);
            }
            loaded.add(new Part(position,collision));
        }
        if (!loaded.get(0).offset().equals(BlockPos.ZERO)) throw invalid("missing origin part: "+id);
        parts=List.copyOf(loaded);
    }

    public static Catalog load() {
        try (var stream=OriginalOpeningVisualDefinition.class.getResourceAsStream(RESOURCE)) {
            if (stream==null) throw new IllegalStateException("Opening visual catalog is missing");
            byte[] bytes=stream.readNBytes(MAX_BYTES+1);
            if (bytes.length>MAX_BYTES) throw invalid("catalog byte budget");
            var document=object(JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)));
            return new Catalog(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),parse(document));
        } catch (Exception failure) { throw new IllegalStateException("Cannot load opening visual catalog",failure); }
    }

    public static List<OriginalOpeningVisualDefinition> parse(JsonObject document) {
        fields(document,Set.of("version","region","blocks"));
        if (integer(document.get("version"))!=1 || !"opening_private_visual_candidate".equals(string(document,"region",64))) throw invalid("catalog version or region");
        JsonArray blocks=array(document.get("blocks"));
        if (blocks.isEmpty() || blocks.size()>128) throw invalid("catalog module count");
        var result=new ArrayList<OriginalOpeningVisualDefinition>();
        var ids=new HashSet<String>();
        int total=0;
        for (JsonElement element:blocks) {
            var definition=new OriginalOpeningVisualDefinition(object(element));
            for (int part=0;part<definition.parts.size();part++) {
                if (!ids.add(definition.partId(part))) throw invalid("duplicate registry id: "+definition.id);
            }
            total+=definition.parts.size();
            if (total>4096) throw invalid("catalog part budget");
            result.add(definition);
        }
        return List.copyOf(result);
    }

    public static BlockPos rotate(BlockPos offset, Direction facing) {
        horizontal(facing);
        return switch (facing) {
            case EAST -> new BlockPos(-offset.getZ(),offset.getY(),offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(),offset.getY(),-offset.getZ());
            case WEST -> new BlockPos(offset.getZ(),offset.getY(),-offset.getX());
            default -> offset;
        };
    }

    private static Box rotate(Box box, Direction facing) {
        return switch (facing) {
            case EAST -> new Box(16-box.maxZ(),box.minY(),box.minX(),16-box.minZ(),box.maxY(),box.maxX());
            case SOUTH -> new Box(16-box.maxX(),box.minY(),16-box.maxZ(),16-box.minX(),box.maxY(),16-box.minZ());
            case WEST -> new Box(box.minZ(),box.minY(),16-box.maxX(),box.maxZ(),box.maxY(),16-box.minX());
            default -> box;
        };
    }

    public static void horizontal(Direction facing) {
        if (facing==null || facing.getAxis()==Direction.Axis.Y) throw invalid("horizontal facing required");
    }

    private static JsonObject object(JsonElement value) { if (value==null || !value.isJsonObject()) throw invalid("object type");return value.getAsJsonObject(); }
    private static JsonArray array(JsonElement value) { if (value==null || !value.isJsonArray()) throw invalid("array type");return value.getAsJsonArray(); }
    private static void fields(JsonObject value, Set<String> keys) { if (!value.keySet().equals(keys)) throw invalid("object fields"); }
    private static String string(JsonObject value, String key, int maximum) { return string(value.get(key),maximum); }
    private static String string(JsonElement value, int maximum) {
        if (value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw invalid("string type");
        String result=value.getAsString();
        if (result.isBlank() || result.length()>maximum) throw invalid("string length");
        return result;
    }
    private static boolean bool(JsonObject value, String key) {
        JsonElement element=value.get(key);
        if (element==null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) throw invalid("boolean type");
        return element.getAsBoolean();
    }
    private static int integer(JsonElement value) {
        if (value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw invalid("integer type");
        try { return value.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException failure) { throw invalid("integer range"); }
    }
    private static double number(JsonElement value) {
        if (value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw invalid("number type");
        double result=value.getAsDouble();
        if (!Double.isFinite(result)) throw invalid("number range");
        return result;
    }
    private static IllegalArgumentException invalid(String detail) { return new IllegalArgumentException("Invalid opening visual catalog: "+detail); }

    public String id() { return id; }
    public String material() { return material; }
    public String category() { return category; }
    public float strength() { return strength; }
    public int light() { return light; }
    public boolean occluding() { return occluding; }
    public List<String> materialIds() { return materialIds; }
    public String sourceGeometry() { return sourceGeometry; }
    public List<Part> parts() { return parts; }
    public String partId(int part) {
        if (part<0 || part>=parts.size()) throw invalid("part index: "+id);
        return id+(part==0?"":"_part_"+part);
    }
}
