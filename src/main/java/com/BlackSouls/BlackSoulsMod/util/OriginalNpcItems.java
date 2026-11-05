package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OriginalNpcItems {
    private static final Map<String, Entry> ENTRIES = load();

    private OriginalNpcItems() {
    }

    public static java.util.List<Entry> values() {
        return ENTRIES.values().stream().sorted(java.util.Comparator.comparingInt(Entry::kind).thenComparingInt(Entry::id)).toList();
    }

    public static Entry get(int kind, int id) {
        return ENTRIES.get(kind + ":" + id);
    }

    public static Item item(int kind, int id) {
        Entry entry = get(kind, id);
        if (entry == null || entry.registry() == null) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(entry.registry());
        return item == null || item == Items.AIR ? null : item;
    }

    private static Map<String, Entry> load() {
        try (InputStream stream = OriginalNpcItems.class.getResourceAsStream(
                "/data/blacksouls/original_npc_items.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing original NPC item mapping");
            }
            Map<String, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : JsonParser.parseReader(new InputStreamReader(stream,
                    StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject json = element.getAsJsonObject();
                Entry entry = new Entry(json.get("kind").getAsInt(), json.get("id").getAsInt(),
                        json.get("name").getAsString(), json.get("price").getAsLong(),
                        json.has("registry") && !json.get("registry").isJsonNull()
                                ? ResourceLocation.tryParse(json.get("registry").getAsString()) : null);
                entries.put(entry.kind() + ":" + entry.id(), entry);
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC item mapping", exception);
        }
    }

    public record Entry(int kind, int id, String name, long price, ResourceLocation registry) {
    }
}
