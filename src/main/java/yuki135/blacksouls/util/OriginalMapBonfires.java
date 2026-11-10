package yuki135.blacksouls.util;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class OriginalMapBonfires {
    private static final Set<BlockPos> POSITIONS = load();
    private OriginalMapBonfires() { }

    public static boolean mirrored(ResourceKey<Level> dimension, BlockPos position) {
        return (dimension.equals(Level.OVERWORLD) || HokoniwaDestination.isHokoniwa(dimension)) && POSITIONS.contains(position);
    }

    private static Set<BlockPos> load() {
        try (var stream = OriginalMapBonfires.class.getResourceAsStream("/data/blacksouls/original_map_bonfires.json")) {
            if (stream == null) throw new IllegalStateException("Missing original map bonfire positions");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<BlockPos> result = new HashSet<>();
            for (var element : json.getAsJsonArray("bonfires")) {
                var row = element.getAsJsonObject();
                result.add(new BlockPos(row.get("x").getAsInt(), row.get("y").getAsInt(), row.get("z").getAsInt()));
            }
            return Set.copyOf(result);
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to load original map bonfire positions", failure);
        }
    }
}
