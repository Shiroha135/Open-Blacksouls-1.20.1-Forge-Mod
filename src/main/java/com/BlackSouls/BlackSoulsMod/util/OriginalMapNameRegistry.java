package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class OriginalMapNameRegistry {
    private static final String RESOURCE_PATH = "/data/blacksouls/original_map_names.json";
    private static final Map<String, String> NAMES = load();

    public static String getDisplayName(String sceneId) {
        if (sceneId == null || sceneId.isBlank()) {
            return "???";
        }
        String normalized = sceneId.trim().toLowerCase(Locale.ROOT);
        return NAMES.getOrDefault(normalized, sceneId.trim());
    }

    private static Map<String, String> load() {
        Map<String, String> names = new HashMap<>();
        try (InputStream stream = OriginalMapNameRegistry.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                return names;
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class
            );
            JsonObject maps = root == null ? null : root.getAsJsonObject("maps");
            if (maps == null) {
                return names;
            }
            for (Map.Entry<String, JsonElement> entry : maps.entrySet()) {
                JsonObject map = entry.getValue().getAsJsonObject();
                String name = map.has("name") ? map.get("name").getAsString().trim() : "";
                if (!name.isEmpty()) {
                    names.put(entry.getKey().toLowerCase(Locale.ROOT), name);
                }
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return names;
    }

    private OriginalMapNameRegistry() {
    }
}
