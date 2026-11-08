package yuki135.blacksouls.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

public final class OriginalBlockDefinition {
    public record Part(BlockPos offset, VoxelShape[] shapes) {
        public VoxelShape shape(Direction facing) {
            return shapes[facing.get2DDataValue()];
        }
    }

    private final String id;
    private final String material;
    private final float strength;
    private final int light;
    private final boolean solid;
    private final List<Part> parts;
    private final List<RegistryObject<OriginalTileBlock>> blocks = new ArrayList<>();

    private OriginalBlockDefinition(JsonObject json) {
        id = json.get("id").getAsString();
        if (!id.matches("original_[a-z0-9_]+")) throw new IllegalArgumentException("Invalid original block id: " + id);
        material = json.get("material").getAsString();
        strength = json.get("strength").getAsFloat();
        light = json.get("light").getAsInt();
        solid = json.get("solid").getAsBoolean();
        if (!Float.isFinite(strength) || strength < 0 || strength > 50 || light < 0 || light > 15) {
            throw new IllegalArgumentException("Invalid original block properties: " + id);
        }
        var offsets = new HashSet<BlockPos>();
        var loaded = new ArrayList<Part>();
        for (var element : json.getAsJsonArray("parts")) {
            JsonObject part = element.getAsJsonObject();
            JsonArray offset = part.getAsJsonArray("offset");
            BlockPos pos = new BlockPos(offset.get(0).getAsInt(), offset.get(1).getAsInt(), offset.get(2).getAsInt());
            if (!offsets.add(pos) || pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0
                    || pos.getX() > 3 || pos.getY() > 4 || pos.getZ() > 3) {
                throw new IllegalArgumentException("Invalid original block part: " + id);
            }
            VoxelShape[] rotated = {Shapes.empty(), Shapes.empty(), Shapes.empty(), Shapes.empty()};
            for (var boxElement : part.getAsJsonArray("collision")) {
                JsonArray box = boxElement.getAsJsonArray();
                double[] values = new double[6];
                for (int i = 0; i < values.length; i++) {
                    values[i] = box.get(i).getAsDouble();
                    if (!Double.isFinite(values[i]) || values[i] < 0 || values[i] > 16) {
                        throw new IllegalArgumentException("Invalid original block collision: " + id);
                    }
                }
                if (values[0] >= values[3] || values[1] >= values[4] || values[2] >= values[5]) {
                    throw new IllegalArgumentException("Empty original block collision: " + id);
                }
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    double[] bounds = rotatedBox(values, facing);
                    int index = facing.get2DDataValue();
                    rotated[index] = Shapes.or(rotated[index], Block.box(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]));
                }
            }
            for (int i = 0; i < rotated.length; i++) rotated[i] = rotated[i].optimize();
            loaded.add(new Part(pos, rotated));
        }
        if (loaded.isEmpty() || loaded.size() > 64 || !loaded.get(0).offset().equals(BlockPos.ZERO)) {
            throw new IllegalArgumentException("Invalid original block structure: " + id);
        }
        parts = List.copyOf(loaded);
    }

    public static List<OriginalBlockDefinition> load() {
        try (InputStream stream = OriginalBlockDefinition.class.getResourceAsStream("/data/blacksouls/original_blocks.json")) {
            if (stream == null) throw new IllegalStateException("Original block catalog is missing");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported original block catalog");
            var entries = new ArrayList<OriginalBlockDefinition>();
            var ids = new HashSet<String>();
            for (var element : json.getAsJsonArray("blocks")) {
                var definition = new OriginalBlockDefinition(element.getAsJsonObject());
                if (!ids.add(definition.id())) throw new IllegalArgumentException("Duplicate original block: " + definition.id());
                entries.add(definition);
            }
            return List.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load original block catalog", exception);
        }
    }

    private static double[] rotatedBox(double[] box, Direction facing) {
        return switch (facing) {
            case EAST -> new double[]{16 - box[5], box[1], box[0], 16 - box[2], box[4], box[3]};
            case SOUTH -> new double[]{16 - box[3], box[1], 16 - box[5], 16 - box[0], box[4], 16 - box[2]};
            case WEST -> new double[]{box[2], box[1], 16 - box[3], box[5], box[4], 16 - box[0]};
            default -> box.clone();
        };
    }

    public static BlockPos rotate(BlockPos offset, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> offset;
        };
    }

    public String id() { return id; }
    public String material() { return material; }
    public float strength() { return strength; }
    public int light() { return light; }
    public boolean solid() { return solid; }
    public List<Part> parts() { return parts; }
    public List<RegistryObject<OriginalTileBlock>> blocks() { return blocks; }
}
