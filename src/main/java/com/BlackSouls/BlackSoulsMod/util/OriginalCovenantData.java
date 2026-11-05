package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OriginalCovenantData {
    private static final Map<Integer, Entry> ENTRIES = load();

    private OriginalCovenantData() {
    }

    public static Entry get(int commonEvent) {
        return ENTRIES.get(commonEvent);
    }

    public static List<Entry> values() {
        return ENTRIES.values().stream().sorted(java.util.Comparator.comparingInt(Entry::commonEvent)).toList();
    }

    public static Entry forSwitch(int id) {
        return ENTRIES.values().stream().filter(entry -> entry.pledgeSwitch() == id).findFirst().orElse(null);
    }

    public static Entry forVariable(int id) {
        return ENTRIES.values().stream().filter(entry -> entry.stageVariable() == id).findFirst().orElse(null);
    }

    private static Map<Integer, Entry> load() {
        try (var stream = OriginalCovenantData.class.getResourceAsStream("/data/blacksouls/original_covenants.json")) {
            if (stream == null) throw new IllegalStateException("Missing original covenant data");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("families")) {
                var row = element.getAsJsonObject();
                var entry = new Entry(row.get("commonEvent").getAsInt(), row.get("firstSpirit").getAsInt(),
                        row.get("pledgeSwitch").getAsInt(), row.get("stageVariable").getAsInt());
                if (entry.commonEvent() < 1 || entry.firstSpirit() < 1 || entry.firstSpirit() > 65
                        || (entry.firstSpirit() - 1) % 4 != 0 || entry.pledgeSwitch() < 1 || entry.stageVariable() < 1
                        || entries.values().stream().anyMatch(other -> other.firstSpirit() == entry.firstSpirit()
                        || other.pledgeSwitch() == entry.pledgeSwitch() || other.stageVariable() == entry.stageVariable())
                        || entries.put(entry.commonEvent(), entry) != null) throw new IllegalArgumentException("Invalid original covenant family");
            }
            if (entries.size() != 17) throw new IllegalArgumentException("Incomplete original covenant families");
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original covenant families", exception);
        }
    }

    public record Entry(int commonEvent, int firstSpirit, int pledgeSwitch, int stageVariable) {
    }
}
