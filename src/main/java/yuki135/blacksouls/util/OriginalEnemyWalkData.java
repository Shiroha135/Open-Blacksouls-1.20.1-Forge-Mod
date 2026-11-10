package yuki135.blacksouls.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class OriginalEnemyWalkData {
    public record Sprite(ResourceLocation texture, int textureWidth, int textureHeight, int characterIndex,
                         int columns, int rows, int frameWidth, int frameHeight, boolean stepAnime,
                         boolean directionFix, int direction, int framesMask, boolean walkAnime, int pattern) { }
    private record Entry(Sprite fallback, Map<Integer, Sprite> maps) { }
    private static final Map<Integer, Entry> ENTRIES = load();

    private OriginalEnemyWalkData() { }

    public static Sprite get(int enemy, int map) {
        var entry = ENTRIES.get(enemy);
        return entry == null ? null : entry.maps().getOrDefault(map, entry.fallback());
    }

    private static Map<Integer, Entry> load() {
        try (var stream = OriginalEnemyWalkData.class.getResourceAsStream("/data/blacksouls/original_enemy_walk_sprites.json")) {
            if (stream == null) throw new IllegalStateException("Missing original enemy walking sprites");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Sprite> sprites = new HashMap<>();
            for (var item : root.getAsJsonObject("sprites").entrySet()) {
                JsonObject row = item.getValue().getAsJsonObject();
                var sprite = new Sprite(ResourceLocation.tryParse(row.get("texture").getAsString()),
                        row.get("textureWidth").getAsInt(), row.get("textureHeight").getAsInt(), row.get("characterIndex").getAsInt(),
                        row.get("columns").getAsInt(), row.get("rows").getAsInt(), row.get("frameWidth").getAsInt(),
                        row.get("frameHeight").getAsInt(), row.get("stepAnime").getAsBoolean(),
                        row.get("directionFix").getAsBoolean(), row.get("direction").getAsInt(), row.get("framesMask").getAsInt(),
                        row.get("walkAnime").getAsBoolean(), row.get("pattern").getAsInt());
                if (sprite.texture() == null || sprite.characterIndex() < 0 || sprite.characterIndex() > 7
                        || sprite.frameWidth() < 1 || sprite.frameHeight() < 1 || sprite.textureWidth() < 1 || sprite.textureHeight() < 1
                        || sprite.framesMask() < 1 || sprite.framesMask() > 4095
                        || !(sprite.columns() == 3 && sprite.rows() == 4 || sprite.columns() == 12 && sprite.rows() == 8)) {
                    throw new IllegalStateException("Invalid enemy walking sprite " + item.getKey());
                }
                sprites.put(item.getKey(), sprite);
            }
            Map<Integer, Entry> entries = new HashMap<>();
            for (var value : root.getAsJsonArray("enemies")) {
                var row = value.getAsJsonObject();
                var fallback = sprites.get(row.get("sprite").getAsString());
                if (fallback == null) throw new IllegalStateException("Unknown enemy walking sprite");
                Map<Integer, Sprite> maps = new HashMap<>();
                for (var variant : row.getAsJsonObject("maps").entrySet()) {
                    var sprite = sprites.get(variant.getValue().getAsString());
                    if (sprite == null) throw new IllegalStateException("Unknown map walking sprite");
                    maps.put(Integer.parseInt(variant.getKey()), sprite);
                }
                if (entries.put(row.get("enemyId").getAsInt(), new Entry(fallback, Map.copyOf(maps))) != null) {
                    throw new IllegalStateException("Duplicate enemy walking sprite");
                }
            }
            return Map.copyOf(entries);
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to load original enemy walking sprites", failure);
        }
    }
}
