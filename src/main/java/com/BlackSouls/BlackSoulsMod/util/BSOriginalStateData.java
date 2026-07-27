package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class BSOriginalStateData {
    private static final Map<Integer, Entry> ENTRIES = loadEntries();

    private BSOriginalStateData() {
    }

    public static Entry get(int stateId) {
        return ENTRIES.get(stateId);
    }

    private static Map<Integer, Entry> loadEntries() {
        ResourceLocation location = new ResourceLocation(
                BlackSouls.MODID, "original_states.json");
        String path = "/data/" + location.getNamespace() + "/" + location.getPath();
        try (InputStream stream = BSOriginalStateData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original state data: " + path);
            }
            JsonArray array = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : array) {
                JsonObject json = element.getAsJsonObject();
                Entry entry = new Entry(
                        json.get("id").getAsInt(),
                        json.get("name").getAsString(),
                        json.get("iconIndex").getAsInt()
                );
                entries.put(entry.id(), entry);
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original state data", exception);
        }
    }

    public record Entry(int id, String name, int iconIndex) {
    }
}
