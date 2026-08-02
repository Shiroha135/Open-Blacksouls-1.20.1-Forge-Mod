package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class OriginalMapSceneRegistry {
    private static final String RESOURCE_PATH = "/data/blacksouls/original_map_scenes.json";
    private static final Map<String, Entry> ENTRIES = load();
    private static final Set<ResourceLocation> SOUND_EVENTS = loadSoundEvents();

    public static Entry get(String sceneId) {
        if (sceneId == null || sceneId.isBlank()) {
            return null;
        }
        return ENTRIES.get(sceneId.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isSceneSound(ResourceLocation soundEvent) {
        return soundEvent != null && SOUND_EVENTS.contains(soundEvent);
    }

    private static Map<String, Entry> load() {
        Map<String, Entry> entries = new HashMap<>();
        try (InputStream stream = OriginalMapSceneRegistry.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                return entries;
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class
            );
            JsonObject maps = root == null ? null : root.getAsJsonObject("maps");
            if (maps == null) {
                return entries;
            }
            for (Map.Entry<String, JsonElement> mapEntry : maps.entrySet()) {
                JsonObject map = mapEntry.getValue().getAsJsonObject();
                ResourceLocation soundEvent = map.has("sound_event")
                        ? ResourceLocation.tryParse(map.get("sound_event").getAsString()) : null;
                if (soundEvent == null) {
                    continue;
                }
                float volume = map.has("volume") ? map.get("volume").getAsFloat() / 100.0F : 1.0F;
                float pitch = map.has("pitch") ? map.get("pitch").getAsFloat() / 100.0F : 1.0F;
                entries.put(mapEntry.getKey().toLowerCase(Locale.ROOT),
                        new Entry(soundEvent, volume, pitch));
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return entries;
    }

    private static Set<ResourceLocation> loadSoundEvents() {
        Set<ResourceLocation> events = new HashSet<>();
        for (Entry entry : ENTRIES.values()) {
            events.add(entry.soundEvent());
        }
        return events;
    }

    public record Entry(ResourceLocation soundEvent, float volume, float pitch) {
    }

    private OriginalMapSceneRegistry() {
    }
}
