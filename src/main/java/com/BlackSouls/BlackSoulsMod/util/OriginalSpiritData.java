package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OriginalSpiritData {
    private static final Map<Integer, Entry> ENTRIES = load();

    private OriginalSpiritData() {
    }

    public static Entry get(int id) {
        return ENTRIES.get(id);
    }

    public static boolean supported(int id) {
        return id >= 1 && id <= 4;
    }

    private static Map<Integer, Entry> load() {
        try (var stream = OriginalSpiritData.class.getResourceAsStream("/data/blacksouls/original_spirits.json")) {
            if (stream == null) throw new IllegalStateException("Missing original spirit data");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("definitions")) {
                var row = element.getAsJsonObject();
                int id = row.get("id").getAsInt();
                var entry = new Entry(id, row.get("actorId").getAsInt(), row.get("name").getAsString(), row.get("description").getAsString());
                if (id < 1 || id > 68 || entries.put(id, entry) != null) throw new IllegalArgumentException("Invalid original spirit identity");
            }
            if (entries.size() != 68 || root.get("playerActor2MaxPartners").getAsInt() != 1) throw new IllegalArgumentException("Incomplete original spirit data");
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original spirit data", exception);
        }
    }

    public record Entry(int id, int actorId, String name, String description) {
    }
}
