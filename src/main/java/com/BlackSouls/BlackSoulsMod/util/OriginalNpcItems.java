package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    public static boolean matches(int kind, int id, ItemStack stack) {
        if (kind < 0 || kind > 2 || id < 1 || stack == null || stack.isEmpty()) return false;
        if (kind == 1) return sourceId(stack) == id;
        Item item = item(kind, id);
        return item != null && stack.is(item);
    }

    public static int sourceId(ItemStack stack) {
        return OriginalWeaponForms.sourceId(stack);
    }

    public static OriginalWeaponForms.WeaponData details(int sourceId) {
        return OriginalWeaponForms.details(sourceId);
    }

    public static ItemStack createStack(int kind, int id, int count) {
        Item item = item(kind, id);
        if (item == null || count < 0) throw new IllegalArgumentException("Invalid original item stack: " + kind + ":" + id);
        if (count == 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, count);
        OriginalWeaponForms.Form form = kind == 1 ? OriginalWeaponForms.get(id) : null;
        if (form != null && form.upgradeLevel() > 0) stack.getOrCreateTag().putInt("bs2_upgrade_level", form.upgradeLevel());
        return stack;
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
                int kind = json.get("kind").getAsInt();
                int id = json.get("id").getAsInt();
                OriginalWeaponForms.Form form = kind == 1 ? OriginalWeaponForms.get(id) : null;
                Entry entry = new Entry(json.get("kind").getAsInt(), json.get("id").getAsInt(),
                        json.get("name").getAsString(), json.get("price").getAsLong(),
                        form != null ? form.registry() : json.has("registry") && !json.get("registry").isJsonNull()
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
