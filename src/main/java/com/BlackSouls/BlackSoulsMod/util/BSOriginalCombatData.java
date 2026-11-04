package com.BlackSouls.BlackSoulsMod.util;

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

public final class BSOriginalCombatData {
    private static final Enemy EMPTY_ENEMY = new Enemy(List.of(), "");
    private static final Database DATABASE = loadEntries();

    private BSOriginalCombatData() {
    }

    public static Enemy enemy(int id) {
        return DATABASE.enemies().getOrDefault(id, EMPTY_ENEMY);
    }

    public static State state(int id) {
        return DATABASE.states().get(id);
    }

    private static Database loadEntries() {
        String path = "/data/blacksouls/original_combat_traits.json";
        try (InputStream stream = BSOriginalCombatData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original combat data: " + path);
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<Integer, Enemy> enemies = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("enemies")) {
                JsonObject json = element.getAsJsonObject();
                enemies.put(integer(json, "id"), new Enemy(features(json), string(json, "note")));
            }
            Map<Integer, State> states = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("states")) {
                JsonObject json = element.getAsJsonObject();
                State state = new State(integer(json, "id"), integer(json, "restriction"),
                        integer(json, "minTurns"), integer(json, "maxTurns"),
                        integer(json, "autoRemovalTiming"), bool(json, "removeAtBattleEnd"),
                        bool(json, "removeByDamage"), integer(json, "chanceByDamage"),
                        string(json, "message3"), string(json, "message4"),
                        features(json), string(json, "note"), bool(json, "removeByRestriction"));
                states.put(state.id(), state);
            }
            return new Database(Map.copyOf(enemies), Map.copyOf(states));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original combat data", exception);
        }
    }

    private static List<Feature> features(JsonObject json) {
        List<Feature> features = new ArrayList<>();
        if (json.has("features")) {
            for (JsonElement element : json.getAsJsonArray("features")) {
                JsonObject feature = element.getAsJsonObject();
                features.add(new Feature(integer(feature, "code"), integer(feature, "dataId"),
                        feature.has("value") ? feature.get("value").getAsDouble() : 0.0D));
            }
        }
        return List.copyOf(features);
    }

    private static int integer(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsInt() : 0;
    }

    private static String string(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    private static boolean bool(JsonObject json, String key) {
        return json.has(key) && json.get(key).getAsBoolean();
    }

    private record Database(Map<Integer, Enemy> enemies, Map<Integer, State> states) {
    }

    public record Enemy(List<Feature> features, String note) {
        public Enemy {
            features = List.copyOf(features);
            note = note == null ? "" : note;
        }
    }

    public record State(int id, int restriction, int minTurns, int maxTurns, int autoRemovalTiming,
                        boolean removeAtBattleEnd, boolean removeByDamage, int chanceByDamage,
                        String message3, String message4, List<Feature> features, String note,
                        boolean removeByRestriction) {
        public State {
            message3 = message3 == null ? "" : message3;
            message4 = message4 == null ? "" : message4;
            features = List.copyOf(features);
            note = note == null ? "" : note;
        }
    }

    public record Feature(int code, int dataId, double value) {
    }
}
