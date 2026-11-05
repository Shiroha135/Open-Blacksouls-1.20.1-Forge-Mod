package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class OriginalNpcShopRules {
    public static final int NORMAL_LIMIT = 99;
    public static final int STORAGE_LIMIT = 999;
    public static final long MAX_GOLD = 99999999L;
    private static final Map<String, Rule> RULES = load();

    private OriginalNpcShopRules() {
    }

    public static Rule get(int kind, int id) {
        return RULES.get(kind + ":" + id);
    }

    public static int category(int kind, int id) {
        Rule rule = get(kind, id);
        if (rule == null) throw new IllegalArgumentException("Unknown original shop item");
        return rule.category(kind);
    }

    private static Map<String, Rule> load() {
        String path = "/data/blacksouls/original_npc_shop_rules.json";
        try (InputStream stream = OriginalNpcShopRules.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing original shop rules");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (integer(root, "version") != 1 || integer(root, "carriedLimit") != NORMAL_LIMIT
                    || integer(root, "defaultStorageLimit") != STORAGE_LIMIT || integer(root, "goldLimit") != MAX_GOLD) {
                throw new IllegalArgumentException("Unexpected original shop limits");
            }
            Map<String, Rule> rules = new HashMap<>();
            for (JsonElement element : root.getAsJsonArray("items")) {
                JsonObject entry = element.getAsJsonObject();
                int kind = integer(entry, "kind");
                int id = integer(entry, "id");
                if (kind < 0 || kind > 2 || id < 1 || !entry.has("itypeId")) {
                    throw new IllegalArgumentException("Invalid original shop item identity");
                }
                Integer type = entry.get("itypeId").isJsonNull() ? null : integer(entry, "itypeId");
                if (kind == 0 ? type == null || type < 1 || type > 2 : type != null) {
                    throw new IllegalArgumentException("Invalid original shop item type");
                }
                if (!entry.get("storageForbidden").isJsonPrimitive()
                        || !entry.getAsJsonPrimitive("storageForbidden").isBoolean()) {
                    throw new IllegalArgumentException("Invalid original shop storage rule");
                }
                Rule rule = new Rule(type, entry.get("storageForbidden").getAsBoolean(), integer(entry, "storageLimit"));
                if (rule.storageLimit() < 0 || rule.storageLimit() > STORAGE_LIMIT
                        || rules.put(kind + ":" + id, rule) != null) {
                    throw new IllegalArgumentException("Invalid or duplicate original shop rule");
                }
            }
            if (rules.size() != 1150) throw new IllegalArgumentException("Incomplete original shop rules");
            return Map.copyOf(rules);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original shop rules", exception);
        }
    }

    private static int integer(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Invalid original shop integer: " + key);
        }
        return value.getAsBigDecimal().intValueExact();
    }

    public record Rule(Integer itypeId, boolean storageForbidden, int storageLimit) {
        public int category(int kind) {
            if (kind < 0 || kind > 2 || kind == 0 && itypeId == null) {
                throw new IllegalArgumentException("Invalid original shop category");
            }
            return kind == 0 && itypeId == 2 ? 3 : kind;
        }
    }
}
