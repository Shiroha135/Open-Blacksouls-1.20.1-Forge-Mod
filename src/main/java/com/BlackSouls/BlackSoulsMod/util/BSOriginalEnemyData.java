package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public final class BSOriginalEnemyData {
    private static final Map<Integer, Entry> ENTRIES = loadEntries();

    private BSOriginalEnemyData() {
    }

    public static Entry get(int id) {
        Entry entry = ENTRIES.get(id);
        return entry != null ? entry : ENTRIES.values().iterator().next();
    }

    public static List<Entry> values() {
        return List.copyOf(ENTRIES.values());
    }

    private static Map<Integer, Entry> loadEntries() {
        ResourceLocation dataLocation = new ResourceLocation(BlackSouls.MODID, "original_enemies.json");
        String path = "/data/" + dataLocation.getNamespace() + "/" + dataLocation.getPath();
        try (InputStream stream = BSOriginalEnemyData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original enemy data: " + path);
            }
            JsonArray array = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : array) {
                JsonObject json = element.getAsJsonObject();
                List<Action> actions = new ArrayList<>();
                for (JsonElement actionElement : json.getAsJsonArray("actions")) {
                    JsonObject action = actionElement.getAsJsonObject();
                    String actionName = action.get("name").getAsString();
                    List<StateEffect> stateEffects = new ArrayList<>();
                    if (action.has("stateEffects")) {
                        for (JsonElement stateEffectElement : action.getAsJsonArray("stateEffects")) {
                            JsonObject stateEffect = stateEffectElement.getAsJsonObject();
                            stateEffects.add(new StateEffect(
                                    stateEffect.get("code").getAsInt(),
                                    stateEffect.get("stateId").getAsInt(),
                                    stateEffect.get("chance").getAsDouble()
                            ));
                        }
                    }
                    actions.add(new Action(
                            action.get("skillId").getAsInt(),
                            actionName,
                            normalizeActionText(action.get("text").getAsString(), actionName),
                            action.get("animationId").getAsInt(),
                            action.get("repeats").getAsInt(),
                            action.get("rating").getAsInt(),
                            action.get("conditionType").getAsInt(),
                            action.get("conditionParam1").getAsDouble(),
                            action.get("conditionParam2").getAsDouble(),
                            action.get("damageType").getAsInt(),
                            action.has("formula") ? action.get("formula").getAsString() : "",
                            action.has("variance") ? action.get("variance").getAsInt() : 20,
                            action.has("critical") && action.get("critical").getAsBoolean(),
                            action.has("hitType") ? action.get("hitType").getAsInt() : 0,
                            action.has("successRate") ? action.get("successRate").getAsInt() : 100,
                            action.has("elementId") ? action.get("elementId").getAsInt() : 0,
                            action.has("scope") ? action.get("scope").getAsInt() : 1,
                            List.copyOf(stateEffects),
                            action.has("followUpSkillId") ? action.get("followUpSkillId").getAsInt() : 0,
                            !action.has("selectable") || action.get("selectable").getAsBoolean()
                    ));
                }
                List<Drop> drops = new ArrayList<>();
                for (JsonElement dropElement : json.getAsJsonArray("drops")) {
                    JsonObject drop = dropElement.getAsJsonObject();
                    drops.add(new Drop(drop.get("item").getAsString(),
                            drop.get("denominator").getAsInt()));
                }
                List<Integer> initialStates = new ArrayList<>();
                if (json.has("initialStates")) {
                    for (JsonElement state : json.getAsJsonArray("initialStates")) {
                        initialStates.add(state.getAsInt());
                    }
                }
                Map<String, Double> elementRates = new LinkedHashMap<>();
                if (json.has("elementRates")) {
                    for (Map.Entry<String, JsonElement> rate
                            : json.getAsJsonObject("elementRates").entrySet()) {
                        elementRates.put(rate.getKey(), rate.getValue().getAsDouble());
                    }
                }
                Entry entry = new Entry(
                        json.get("id").getAsInt(),
                        json.get("name").getAsString(),
                        new ResourceLocation(json.get("texture").getAsString()),
                        json.get("textureWidth").getAsInt(),
                        json.get("textureHeight").getAsInt(),
                        json.get("health").getAsDouble(),
                        json.get("mp").getAsDouble(),
                        json.get("attack").getAsDouble(),
                        json.get("defense").getAsDouble(),
                        json.get("magicAttack").getAsDouble(),
                        json.get("magicDefense").getAsDouble(),
                        json.get("agility").getAsDouble(),
                        json.get("luck").getAsDouble(),
                        json.get("souls").getAsLong(),
                        json.get("movementSpeed").getAsDouble(),
                        json.get("worldRenderHeight").getAsFloat(),
                        json.get("shadowRadius").getAsFloat(),
                        json.get("primaryColor").getAsInt(),
                        json.get("secondaryColor").getAsInt(),
                        normalizeActionText(json.get("attackText").getAsString(), findPrimaryActionName(actions)),
                        json.get("attackAnimationId").getAsInt(),
                        json.get("attackRepeats").getAsInt(),
                        List.copyOf(actions),
                        List.copyOf(drops),
                        List.copyOf(initialStates),
                        json.get("spawnable").getAsBoolean(),
                        json.has("actionCount") ? Math.max(1, json.get("actionCount").getAsInt()) : 1,
                        json.has("collapseType") ? Math.max(0, json.get("collapseType").getAsInt()) : 0,
                        Map.copyOf(elementRates)
                );
                entries.put(entry.id(), entry);
            }
            if (entries.isEmpty()) {
                throw new IllegalStateException("Original enemy database is empty");
            }
            return entries;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original enemy database", exception);
        }
    }

    private static String normalizeActionText(String text, String actionName) {
        String normalized = text == null ? "" : text.replace("???", "").trim();
        String name = actionName == null ? "" : actionName.trim();
        if (normalized.isEmpty()) {
            normalized = name.isEmpty() ? "攻击" : name;
        }
        while (normalized.endsWith("！") || normalized.endsWith("!")
                || normalized.endsWith("。")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private static String findPrimaryActionName(List<Action> actions) {
        return actions.stream()
                .max((left, right) -> Integer.compare(left.rating(), right.rating()))
                .map(Action::name)
                .orElse("攻击");
    }

    public record Entry(int id, String name, ResourceLocation texture, int textureWidth, int textureHeight,
                        double health, double mp, double attack, double defense, double magicAttack,
                        double magicDefense, double agility, double luck, long souls, double movementSpeed,
                        float worldRenderHeight, float shadowRadius, int primaryColor, int secondaryColor,
                         String attackText, int attackAnimationId, int attackRepeats, List<Action> actions,
                         List<Drop> drops, List<Integer> initialStates, boolean spawnable,
                         int actionCount, int collapseType, Map<String, Double> elementRates) {
        public float aspectRatio() {
            return (float) textureWidth / Math.max(1, textureHeight);
        }

        public Action findAction(int skillId) {
            return actions.stream().filter(action -> action.skillId() == skillId)
                    .findFirst().orElse(null);
        }
    }

    public record Action(int skillId, String name, String text, int animationId, int repeats, int rating,
                         int conditionType, double conditionParam1, double conditionParam2, int damageType,
                         String formula, int variance, boolean critical, int hitType, int successRate,
                         int elementId, int scope, List<StateEffect> stateEffects,
                         int followUpSkillId, boolean selectable) {
    }

    public record StateEffect(int code, int stateId, double chance) {
    }

    public record Drop(String item, int denominator) {
    }
}
