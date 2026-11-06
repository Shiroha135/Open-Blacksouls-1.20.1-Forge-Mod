package yuki135.blacksouls.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OriginalSynthesisRecipes {
    private static final Map<Integer, Recipe> RECIPES = loadRecipes();
    private static final Map<Integer, Weapon> WEAPONS = loadWeapons();
    private static final Map<String, List<Integer>> UNLOCK_SCRIPTS = loadUnlockScripts();

    private OriginalSynthesisRecipes() {
    }

    public static Recipe get(int kind, int id) {
        return kind == 1 ? RECIPES.get(id) : null;
    }

    public static List<Recipe> values() {
        return RECIPES.values().stream().sorted(java.util.Comparator.comparingInt(Recipe::id)).toList();
    }

    public static Weapon weapon(int id) {
        return WEAPONS.get(id);
    }

    public static List<Integer> unlockScript(String script) {
        return UNLOCK_SCRIPTS.get(script);
    }

    private static Map<String, List<Integer>> loadUnlockScripts() {
        Map<String, List<Integer>> scripts = new LinkedHashMap<>();
        for (JsonElement value : resource("original_synthesis_scripts.json").getAsJsonArray("normalBlocks")) {
            String script = value.getAsString();
            if (!script.matches("w_recipe_switch_on\\([1-9][0-9]*\\)(?:\\nw_recipe_switch_on\\([1-9][0-9]*\\))*")) {
                throw new IllegalArgumentException("Invalid original synthesis script");
            }
            List<Integer> ids = script.lines().map(line -> Integer.parseInt(line.substring(19, line.length() - 1))).toList();
            if (ids.stream().anyMatch(id -> !RECIPES.containsKey(id)) || scripts.putIfAbsent(script, ids) != null) {
                throw new IllegalArgumentException("Invalid or duplicate original synthesis script");
            }
        }
        if (scripts.size() != 42) throw new IllegalArgumentException("Incomplete original synthesis scripts");
        return Map.copyOf(scripts);
    }

    private static JsonObject resource(String name) {
        try (var stream = OriginalSynthesisRecipes.class.getResourceAsStream("/data/blacksouls/" + name)) {
            if (stream == null) throw new IllegalStateException("Missing original synthesis resource");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read original synthesis resource", exception);
        }
    }

    private static Map<Integer, Recipe> loadRecipes() {
        JsonObject root = resource("original_synthesis_recipes.json");
        if (integer(root, "schemaVersion") != 1 || root.getAsJsonObject("configuration").get("costView").getAsBoolean()) {
            throw new IllegalArgumentException("Invalid original synthesis configuration");
        }
        Map<Integer, Recipe> recipes = new LinkedHashMap<>();
        for (JsonElement value : root.getAsJsonArray("recipes")) {
            JsonObject row = value.getAsJsonObject();
            int kind = integer(row, "kind");
            int id = integer(row, "id");
            long cost = row.get("cost").getAsBigDecimal().longValueExact();
            if (kind != 1 || id < 1 || cost != 0L || OriginalNpcItems.get(kind, id) == null) {
                throw new IllegalArgumentException("Invalid original synthesis recipe");
            }
            List<Material> materials = new ArrayList<>();
            for (JsonElement material : row.getAsJsonArray("materials")) {
                JsonObject item = material.getAsJsonObject();
                int itemKind = integer(item, "kind");
                int itemId = integer(item, "id");
                int quantity = integer(item, "quantity");
                if (quantity < 1 || quantity > 99 || OriginalNpcItems.get(itemKind, itemId) == null) {
                    throw new IllegalArgumentException("Invalid original synthesis material");
                }
                materials.add(new Material(itemKind, itemId, quantity));
            }
            if (materials.size() != 2 || materials.get(0).kind() != 0 || materials.get(1).kind() != 1
                    || recipes.putIfAbsent(id, new Recipe(kind, id, cost, List.copyOf(materials))) != null) {
                throw new IllegalArgumentException("Invalid original synthesis material sequence");
            }
        }
        if (recipes.size() != 310) throw new IllegalArgumentException("Incomplete original synthesis recipes");
        return Map.copyOf(recipes);
    }

    private static Map<Integer, Weapon> loadWeapons() {
        JsonObject root = resource("original_weapon_source.json");
        if (integer(root, "schemaVersion") != 1) throw new IllegalArgumentException("Invalid original weapon data");
        Map<Integer, Weapon> weapons = new LinkedHashMap<>();
        for (JsonElement value : root.getAsJsonArray("weapons")) {
            JsonObject row = value.getAsJsonObject();
            List<Integer> parameters = new ArrayList<>();
            for (JsonElement parameter : row.getAsJsonArray("params")) parameters.add(parameter.getAsBigDecimal().intValueExact());
            if (parameters.size() != 8) throw new IllegalArgumentException("Invalid original weapon parameters");
            int id = integer(row, "id");
            if (id < 1 || weapons.putIfAbsent(id, new Weapon(id, row.get("name").getAsString(),
                    row.get("description").getAsString(), List.copyOf(parameters), integer(row, "etypeId"),
                    integer(row, "wtypeId"))) != null) throw new IllegalArgumentException("Invalid original weapon identity");
        }
        if (weapons.size() != 500) throw new IllegalArgumentException("Incomplete original weapon source");
        return Map.copyOf(weapons);
    }

    private static int integer(JsonObject row, String key) {
        return row.get(key).getAsBigDecimal().intValueExact();
    }

    public record Material(int kind, int id, int quantity) {
    }

    public record Recipe(int kind, int id, long cost, List<Material> materials) {
    }

    public record Weapon(int id, String name, String description, List<Integer> parameters, int equipmentType, int weaponType) {
    }
}
