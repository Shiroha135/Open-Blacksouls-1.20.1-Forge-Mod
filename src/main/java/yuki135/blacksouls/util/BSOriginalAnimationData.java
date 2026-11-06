package yuki135.blacksouls.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BSOriginalAnimationData {
    public static final int SOURCE_RATE = 4;
    private static final int MAX_FRAME_MAX = (360_000 - 1) / SOURCE_RATE;
    private static final Map<Integer, Entry> ENTRIES = loadEntries();

    private BSOriginalAnimationData() {
    }

    public static Entry get(int id) {
        return ENTRIES.get(id);
    }

    private static Map<Integer, Entry> loadEntries() {
        String path = "/data/blacksouls/original_map_animations.json";
        try (InputStream stream = BSOriginalAnimationData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original map animation data: " + path);
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.get("sourceSha256").getAsString().matches("[0-9a-f]{64}")
                    || exactInt(root, "sourceRate") != SOURCE_RATE) {
                throw new IllegalStateException("Invalid original animation source metadata");
            }
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("animations")) {
                JsonObject json = element.getAsJsonObject();
                Entry entry = new Entry(exactInt(json, "id"), exactInt(json, "frameMax"),
                        exactInt(json, "position"));
                if (entries.put(entry.id(), entry) != null) {
                    throw new IllegalStateException("Duplicate original animation id: " + entry.id());
                }
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original map animation data", exception);
        }
    }

    private static int exactInt(JsonObject json, String key) {
        return json.get(key).getAsBigDecimal().intValueExact();
    }

    public record Entry(int id, int frameMax, int position) {
        public Entry {
            if (id < 1 || frameMax < 1 || frameMax > MAX_FRAME_MAX || position < 0 || position > 3) {
                throw new IllegalArgumentException("Invalid original map animation entry: " + id);
            }
        }
    }
}
