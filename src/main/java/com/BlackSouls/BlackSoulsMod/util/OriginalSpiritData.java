package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Pattern;

public final class OriginalSpiritData {
    private static final Map<Integer, Entry> ENTRIES = load();

    private OriginalSpiritData() {
    }

    public static Entry get(int id) {
        return ENTRIES.get(id);
    }

    public static boolean supported(int id) {
        return ENTRIES.containsKey(id);
    }

    public static boolean nativeStatFeature(BSOriginalCombatData.Feature feature) {
        return feature.code() == 21
                || feature.code() == 22 && switch (feature.dataId()) {
                    case 1, 2, 4, 7, 8 -> true;
                    default -> false;
                }
                || feature.code() == 23 && (feature.dataId() == 0 || feature.dataId() == 4);
    }

    private static Map<Integer, Entry> load() {
        try (var stream = OriginalSpiritData.class.getResourceAsStream("/data/blacksouls/original_spirits.json")) {
            if (stream == null) throw new IllegalStateException("Missing original spirit data");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("definitions")) {
                var row = element.getAsJsonObject();
                int id = row.get("id").getAsInt();
                int actorId = row.get("actorId").getAsInt();
                List<Integer> parameters = new ArrayList<>();
                for (var parameter : row.getAsJsonArray("parameters")) parameters.add(parameter.getAsInt());
                if (parameters.size() != 8) throw new IllegalArgumentException("Invalid original spirit parameters");
                List<BSOriginalCombatData.Feature> features = new ArrayList<>();
                for (var raw : row.getAsJsonArray("features")) {
                    var feature = raw.getAsJsonObject();
                    features.add(new BSOriginalCombatData.Feature(feature.get("code").getAsInt(),
                            feature.get("data_id").getAsInt(), feature.get("value").getAsDouble()));
                }
                String note = root.has("actorNotes") && root.getAsJsonObject("actorNotes").has(String.valueOf(actorId))
                        ? root.getAsJsonObject("actorNotes").get(String.valueOf(actorId)).getAsString() : "";
                var matcher = Pattern.compile("<逃走成功率増加[:：](\\S+)>").matcher(note);
                int escapeBonus = matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
                int affinity = root.getAsJsonObject("playerActor2Affinities").get(String.valueOf(id)).getAsInt();
                if (affinity < 0 || affinity > 5) throw new IllegalArgumentException("Invalid original spirit affinity");
                var entry = new Entry(id, actorId, row.get("name").getAsString(), row.get("description").getAsString(),
                        List.copyOf(features), escapeBonus, List.copyOf(parameters), affinity);
                if (id < 1 || id > 68 || entries.put(id, entry) != null) throw new IllegalArgumentException("Invalid original spirit identity");
            }
            if (entries.size() != 68 || root.get("playerActor2MaxPartners").getAsInt() != 1) throw new IllegalArgumentException("Incomplete original spirit data");
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original spirit data", exception);
        }
    }

    public record Entry(int id, int actorId, String name, String description,
                        List<BSOriginalCombatData.Feature> features, int escapeBonus, List<Integer> parameters, int affinity) {
        public Entry {
            features = List.copyOf(features);
            parameters = List.copyOf(parameters);
        }

        public double rate(int code, int dataId) {
            double result = 1.0D;
            for (var feature : features) {
                if (feature.code() == code && feature.dataId() == dataId) result *= feature.value();
            }
            return result;
        }

        public double sum(int code, int dataId) {
            return features.stream().filter(feature -> feature.code() == code && feature.dataId() == dataId)
                    .mapToDouble(BSOriginalCombatData.Feature::value).sum();
        }

        public boolean hasFeature(int code, int dataId) {
            return features.stream().anyMatch(feature -> feature.code() == code && feature.dataId() == dataId);
        }

        public int parameterAddition(int parameter) {
            double value = Math.floor(Math.max(parameter == 1 ? 0.0D : 1.0D,
                    parameters.get(parameter) * rate(21, parameter))) - 1.0D;
            double affinityRate = switch (affinity) {
                case 0 -> 0.0D;
                case 1 -> 0.5D;
                case 2 -> 0.8D;
                case 3 -> 1.0D;
                case 4 -> 1.2D;
                default -> 1.5D;
            };
            return (int) (value * affinityRate);
        }

        public List<BSOriginalCombatData.Feature> battleFeatures() {
            return features.stream().filter(feature -> !nativeStatFeature(feature)).toList();
        }
    }
}
