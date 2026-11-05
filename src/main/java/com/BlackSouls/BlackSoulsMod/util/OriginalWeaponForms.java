package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OriginalWeaponForms {
    private static final String UPGRADE_TAG = "bs2_upgrade_level";
    private static final Catalog CATALOG = load();

    private OriginalWeaponForms() {
    }

    public static Form get(int sourceId) {
        return CATALOG.forms().get(sourceId);
    }

    public static WeaponData details(int sourceId) {
        return CATALOG.weapons().get(sourceId);
    }

    public static int sourceId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        ResourceLocation registry = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registry == null) return -1;
        Map<Integer, Integer> levels = CATALOG.identities().get(registry);
        if (levels == null) return -1;
        int level = 0;
        if (stack.hasTag() && stack.getTag().contains(UPGRADE_TAG)) {
            if (!stack.getTag().contains(UPGRADE_TAG, Tag.TAG_INT)) return -1;
            level = stack.getTag().getInt(UPGRADE_TAG);
        }
        return levels.getOrDefault(level, -1);
    }

    private static Catalog load() {
        try (InputStream stream = OriginalWeaponForms.class.getResourceAsStream(
                "/data/blacksouls/original_weapon_forms.json")) {
            if (stream == null) throw new IllegalStateException("Missing original weapon forms");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (integer(root, "version") != 1 || !string(root, "sourceSha256").matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Invalid original weapon form version");
            }
            Map<Integer, WeaponData> weapons = new LinkedHashMap<>();
            for (JsonElement value : root.getAsJsonArray("weapons")) {
                JsonObject row = value.getAsJsonObject();
                int id = integer(row, "id");
                List<Integer> params = new ArrayList<>();
                for (JsonElement parameter : row.getAsJsonArray("params")) params.add(integer(parameter));
                WeaponData weapon = new WeaponData(id, string(row, "name"), string(row, "description"), params,
                        integer(row, "wtypeId"), integer(row, "etypeId"));
                if (id < 1 || id > 500 || params.size() != 8 || weapon.wtypeId() < 0 || weapon.etypeId() < 0
                        || weapons.putIfAbsent(id, weapon) != null) throw new IllegalArgumentException("Invalid original weapon data: " + id);
            }
            Map<Integer, Form> forms = new LinkedHashMap<>();
            Map<ResourceLocation, Map<Integer, Integer>> identities = new LinkedHashMap<>();
            for (JsonElement value : root.getAsJsonArray("forms")) {
                JsonObject row = value.getAsJsonObject();
                int id = integer(row, "sourceId");
                int level = integer(row, "upgradeLevel");
                ResourceLocation registry = ResourceLocation.tryParse(string(row, "registry"));
                WeaponData weapon = weapons.get(id);
                if (registry == null || weapon == null || weapon.name().isBlank() || level < 0 || level > 9
                        || forms.putIfAbsent(id, new Form(id, registry, level)) != null
                        || identities.computeIfAbsent(registry, ignored -> new LinkedHashMap<>()).putIfAbsent(level, id) != null) {
                    throw new IllegalArgumentException("Invalid original weapon form: " + id);
                }
            }
            identities.replaceAll((registry, levels) -> {
                if (!levels.containsKey(0)) throw new IllegalArgumentException("Missing base weapon form: " + registry);
                return Map.copyOf(levels);
            });
            return new Catalog(Map.copyOf(forms), Map.copyOf(weapons), Map.copyOf(identities));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original weapon forms", exception);
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Invalid weapon string: " + key);
        }
        return value.getAsString();
    }

    private static int integer(JsonObject object, String key) {
        return integer(object.get(key));
    }

    private static int integer(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Invalid weapon integer");
        }
        return value.getAsBigDecimal().intValueExact();
    }

    public record Form(int sourceId, ResourceLocation registry, int upgradeLevel) {
    }

    public record WeaponData(int id, String name, String description, List<Integer> params, int wtypeId, int etypeId) {
        public WeaponData {
            params = List.copyOf(params);
        }
    }

    private record Catalog(Map<Integer, Form> forms, Map<Integer, WeaponData> weapons,
                           Map<ResourceLocation, Map<Integer, Integer>> identities) {
    }
}
