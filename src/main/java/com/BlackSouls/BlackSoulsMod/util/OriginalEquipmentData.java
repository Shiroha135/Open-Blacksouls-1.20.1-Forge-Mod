package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OriginalEquipmentData {
    private static final Catalog CATALOG = load();

    private OriginalEquipmentData() {
    }

    public static Equipment get(int kind, int id) {
        return CATALOG.equipment().get(kind + ":" + id);
    }

    public static List<Integer> playerSlots() {
        return CATALOG.slots();
    }

    public static boolean playerEquippable(Equipment output, List<Equipment> equipped) {
        List<Feature> features = new ArrayList<>(CATALOG.playerFeatures());
        for (Equipment item : equipped) if (item != null) features.addAll(item.features());
        if (features.stream().anyMatch(feature -> feature.code() == 54 && feature.id() == output.equipmentType())) return false;
        int code = output.kind() == 1 ? 51 : 52;
        return features.stream().anyMatch(feature -> feature.code() == code && feature.id() == output.typeId());
    }

    private static Catalog load() {
        try (var stream = OriginalEquipmentData.class.getResourceAsStream("/data/blacksouls/original_equipment_status.json")) {
            if (stream == null) throw new IllegalStateException("Missing original equipment status data");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (integer(root, "version") != 1 || integer(root, "playerActorId") != 2 || integer(root, "playerClassId") != 1) {
                throw new IllegalArgumentException("Invalid original equipment status catalog");
            }
            List<Integer> slots = integers(root.getAsJsonArray("playerSlots"));
            if (!slots.equals(List.of(0, 1, 2, 3, 4, 4, 4, 4))) throw new IllegalArgumentException("Invalid original actor slots");
            Map<String, Equipment> equipment = new LinkedHashMap<>();
            for (JsonElement value : root.getAsJsonArray("equipment")) {
                JsonObject row = value.getAsJsonObject();
                int kind = integer(row, "kind"), id = integer(row, "id");
                List<Integer> parameters = integers(row.getAsJsonArray("parameters"));
                int equipmentType = integer(row, "equipmentType"), typeId = integer(row, "typeId");
                if (kind < 1 || kind > 2 || id < 1 || parameters.size() != 8 || equipmentType < 0 || equipmentType > 4 || typeId < 0
                        || equipment.putIfAbsent(kind + ":" + id, new Equipment(kind, id, row.get("name").getAsString(),
                        row.get("description").getAsString(), parameters, equipmentType, typeId, features(row))) != null) {
                    throw new IllegalArgumentException("Invalid original equipment identity");
                }
            }
            if (equipment.size() != 760) throw new IllegalArgumentException("Incomplete original equipment status data");
            return new Catalog(Map.copyOf(equipment), slots, features(root));
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Failed to load original equipment status data", failure);
        }
    }

    private static List<Feature> features(JsonObject row) {
        List<Feature> result = new ArrayList<>();
        for (JsonElement value : row.getAsJsonArray("features")) {
            JsonObject feature = value.getAsJsonObject();
            result.add(new Feature(integer(feature, "code"), integer(feature, "id")));
        }
        return List.copyOf(result);
    }

    private static List<Integer> integers(Iterable<JsonElement> values) {
        List<Integer> result = new ArrayList<>();
        for (JsonElement value : values) result.add(value.getAsBigDecimal().intValueExact());
        return List.copyOf(result);
    }

    private static int integer(JsonObject row, String key) {
        return row.get(key).getAsBigDecimal().intValueExact();
    }

    public record Equipment(int kind, int id, String name, String description, List<Integer> parameters,
                            int equipmentType, int typeId, List<Feature> features) {
        public Equipment {
            parameters = List.copyOf(parameters);
            features = List.copyOf(features);
        }
    }

    public record Feature(int code, int id) {
    }

    private record Catalog(Map<String, Equipment> equipment, List<Integer> slots, List<Feature> playerFeatures) {
    }
}
