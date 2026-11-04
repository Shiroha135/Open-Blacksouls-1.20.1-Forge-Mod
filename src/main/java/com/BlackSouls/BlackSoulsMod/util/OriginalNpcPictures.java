package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OriginalNpcPictures {
    private static final Map<String, Entry> ENTRIES = loadEntries();

    private OriginalNpcPictures() {
    }

    public static Entry get(String originalName) {
        return originalName == null ? null : ENTRIES.get(originalName);
    }

    private static Map<String, Entry> loadEntries() {
        String path = "/data/blacksouls/original_npc_pictures.json";
        try (InputStream stream = OriginalNpcPictures.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original NPC picture allowlist: " + path);
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("pictures")) {
                JsonObject json = element.getAsJsonObject();
                Entry entry = new Entry(json.get("name").getAsString(), json.get("texture").getAsString(),
                        json.get("width").getAsBigDecimal().intValueExact(),
                        json.get("height").getAsBigDecimal().intValueExact(), json.get("sourceSha256").getAsString());
                if (entries.put(entry.name(), entry) != null) {
                    throw new IllegalStateException("Duplicate original NPC picture name: " + entry.name());
                }
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC picture allowlist", exception);
        }
    }

    public record Entry(String name, String texture, int width, int height, String sourceSha256) {
        public Entry {
            if (name == null || name.isBlank() || name.length() > 256 || name.contains("/")
                    || name.contains("\\") || name.contains("..") || texture == null
                    || !texture.matches("blacksouls:textures/gui/original_pictures/[a-z0-9][a-z0-9_-]*\\.png")
                    || width < 1 || height < 1 || width > 16384 || height > 16384
                    || (long) width * height > 67_108_864L || sourceSha256 == null
                    || !sourceSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Invalid original NPC picture entry: " + name);
            }
        }
    }
}
