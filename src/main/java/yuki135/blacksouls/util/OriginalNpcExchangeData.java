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

public final class OriginalNpcExchangeData {
    private static final Map<String, List<Material>> RECIPES = load();

    private OriginalNpcExchangeData() {
    }

    public static List<Material> materials(int kind, int id) {
        return RECIPES.getOrDefault(kind + ":" + id, List.of());
    }

    private static Map<String, List<Material>> load() {
        try (var stream = OriginalNpcExchangeData.class.getResourceAsStream("/data/blacksouls/original_npc_exchange.json")) {
            if (stream == null) throw new IllegalStateException("Missing original exchange recipes");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (integer(root, "version") != 1 || integer(root, "switchId") != 935) {
                throw new IllegalArgumentException("Invalid original exchange configuration");
            }
            Map<String, List<Material>> recipes = new LinkedHashMap<>();
            for (JsonElement value : root.getAsJsonArray("recipes")) {
                JsonObject row = value.getAsJsonObject();
                int kind = integer(row, "kind");
                int id = integer(row, "id");
                var output = OriginalNpcItems.get(kind, id);
                if (output == null || !output.name().equals(row.get("name").getAsString())) {
                    throw new IllegalArgumentException("Invalid original exchange output identity");
                }
                List<Material> materials = new ArrayList<>();
                java.util.Set<String> identities = new java.util.HashSet<>();
                for (JsonElement entry : row.getAsJsonArray("materials")) {
                    JsonObject material = entry.getAsJsonObject();
                    int materialKind = integer(material, "kind");
                    int materialId = integer(material, "id");
                    int quantity = integer(material, "quantity");
                    if (materialKind < 0 || materialKind > 2 || materialId < 1 || quantity < 1
                            || OriginalNpcItems.get(materialKind, materialId) == null
                            || !identities.add(materialKind + ":" + materialId)) {
                        throw new IllegalArgumentException("Invalid original exchange material");
                    }
                    materials.add(new Material(materialKind, materialId, quantity));
                }
                if (materials.isEmpty() || recipes.putIfAbsent(kind + ":" + id, List.copyOf(materials)) != null) {
                    throw new IllegalArgumentException("Invalid or duplicate original exchange recipe");
                }
            }
            if (recipes.size() != 22) throw new IllegalArgumentException("Incomplete original exchange recipes");
            return Map.copyOf(recipes);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read original exchange recipes", exception);
        }
    }

    private static int integer(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Invalid original exchange integer: " + key);
        }
        return value.getAsBigDecimal().intValueExact();
    }

    public record Material(int kind, int id, int quantity) {
    }
}
