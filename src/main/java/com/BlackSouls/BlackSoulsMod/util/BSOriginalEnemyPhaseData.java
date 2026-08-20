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
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("removal")
public final class BSOriginalEnemyPhaseData {
    private static final Map<Integer, Transition> TRANSITIONS = loadTransitions();
    private static final Set<Integer> HIDDEN_SPAWN_VARIANTS = Set.of(
            553, 554,
            565,
            571, 572, 573, 574, 575, 576, 577,
            580, 581, 582, 583, 584, 585, 586
    );

    private BSOriginalEnemyPhaseData() {
    }

    public static Transition get(int profileId) {
        return TRANSITIONS.get(profileId);
    }

    public static boolean hasNext(int profileId) {
        return TRANSITIONS.containsKey(profileId);
    }

    public static boolean isPhaseSuccessor(int profileId) {
        return TRANSITIONS.values().stream()
                .anyMatch(transition -> transition.to() == profileId);
    }

    public static boolean shouldShowSpawnEgg(int profileId) {
        return !isPhaseSuccessor(profileId) && !HIDDEN_SPAWN_VARIANTS.contains(profileId);
    }

    public static int countPhasesFrom(int profileId) {
        int count = 1;
        int current = profileId;
        Set<Integer> visited = new HashSet<>();
        while (visited.add(current)) {
            Transition transition = TRANSITIONS.get(current);
            if (transition == null) {
                break;
            }
            count++;
            current = transition.to();
        }
        return count;
    }

    public static int finalProfileId(int profileId) {
        int current = profileId;
        Set<Integer> visited = new HashSet<>();
        while (visited.add(current)) {
            Transition transition = TRANSITIONS.get(current);
            if (transition == null) {
                break;
            }
            current = transition.to();
        }
        return current;
    }

    private static Map<Integer, Transition> loadTransitions() {
        ResourceLocation dataLocation = new ResourceLocation(
                BlackSouls.MODID, "original_enemy_phases.json");
        String path = "/data/" + dataLocation.getNamespace() + "/" + dataLocation.getPath();
        try (InputStream stream = BSOriginalEnemyPhaseData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original enemy phase data: " + path);
            }
            JsonArray array = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            Map<Integer, Transition> transitions = new LinkedHashMap<>();
            for (JsonElement element : array) {
                JsonObject json = element.getAsJsonObject();
                Transition transition = new Transition(
                        json.get("from").getAsInt(),
                        json.get("to").getAsInt(),
                        json.get("thresholdPercent").getAsDouble(),
                        json.get("recoverAll").getAsBoolean(),
                        json.get("troopId").getAsInt()
                );
                transitions.putIfAbsent(transition.from(), transition);
            }
            return Map.copyOf(transitions);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original enemy phase data", exception);
        }
    }

    public record Transition(int from, int to, double thresholdPercent,
                             boolean recoverAll, int troopId) {
    }
}
