package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BSOriginalSkillData {
    private static final Map<Integer, Entry> ENTRIES = loadEntries();

    private BSOriginalSkillData() {
    }

    public static Entry get(int skillId) {
        return ENTRIES.get(skillId);
    }

    private static Map<Integer, Entry> loadEntries() {
        String path = "/data/blacksouls/original_skills.json";
        try (InputStream stream = BSOriginalSkillData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original skill data: " + path);
            }
            JsonArray array = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : array) {
                JsonObject json = element.getAsJsonObject();
                List<Effect> effects = new ArrayList<>();
                if (json.has("effects")) {
                    for (JsonElement effectElement : json.getAsJsonArray("effects")) {
                        JsonObject effect = effectElement.getAsJsonObject();
                        effects.add(new Effect(integer(effect, "code"), integer(effect, "dataId"),
                                number(effect, "value1"), number(effect, "value2")));
                    }
                }
                Entry entry = new Entry(integer(json, "id"), integer(json, "mpCost"),
                        integer(json, "tpCost"), integer(json, "speed"), string(json, "note"),
                        integer(json, "cooldown"), number(json, "guardIgnoreRate"),
                        json.has("damageRatePiercing") && json.get("damageRatePiercing").getAsBoolean(),
                        effects, integer(json, "skillTypeId"));
                entries.put(entry.id(), entry);
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original skill data", exception);
        }
    }

    private static int integer(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsInt() : 0;
    }

    private static double number(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsDouble() : 0.0D;
    }

    private static String string(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    public record Entry(int id, int mpCost, int tpCost, int speed, String note, int cooldown,
                        double guardIgnoreRate, boolean damageRatePiercing, List<Effect> effects, int skillTypeId) {
        public Entry(int id, int mpCost, int tpCost, int speed, String note, int cooldown,
                     double guardIgnoreRate, boolean damageRatePiercing, List<Effect> effects) {
            this(id, mpCost, tpCost, speed, note, cooldown, guardIgnoreRate, damageRatePiercing, effects, 0);
        }
        public Entry {
            note = note == null ? "" : note;
            effects = List.copyOf(effects);
        }
    }

    public record Effect(int code, int dataId, double value1, double value2) {
    }
}
