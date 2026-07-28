package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
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
import net.minecraft.resources.ResourceLocation;

public final class BSOriginalBattleProfileData {
    private static final Map<Integer, Entry> ENTRIES = loadEntries();

    private BSOriginalBattleProfileData() {
    }

    public static Entry get(int profileId) {
        Entry entry = ENTRIES.get(profileId);
        if (entry != null) {
            return entry;
        }
        return new Entry(profileId, 0, List.of(new Member(profileId, List.of())),
                null, 1.0F, 1.0F, null, 544, 416,
                null, 544, 416, List.of(), 0);
    }

    private static Map<Integer, Entry> loadEntries() {
        ResourceLocation location = new ResourceLocation(
                BlackSouls.MODID, "original_enemy_battle_profiles.json");
        String path = "/data/" + location.getNamespace() + "/" + location.getPath();
        try (InputStream stream = BSOriginalBattleProfileData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original battle profile data: " + path);
            }
            JsonArray array = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : array) {
                JsonObject json = element.getAsJsonObject();
                List<Member> members = new ArrayList<>();
                for (JsonElement memberElement : json.getAsJsonArray("members")) {
                    JsonObject member = memberElement.getAsJsonObject();
                    List<Integer> states = new ArrayList<>();
                    if (member.has("states")) {
                        for (JsonElement state : member.getAsJsonArray("states")) {
                            states.add(state.getAsInt());
                        }
                    }
                    members.add(new Member(member.get("profileId").getAsInt(), List.copyOf(states)));
                }
                Entry entry = new Entry(
                        json.get("profileId").getAsInt(),
                        json.get("troopId").getAsInt(),
                        List.copyOf(members),
                        resource(json, "bgm"),
                        json.get("bgmVolume").getAsFloat(),
                        json.get("bgmPitch").getAsFloat(),
                        resource(json, "battleback1"),
                        integer(json, "battleback1Width", 544),
                        integer(json, "battleback1Height", 416),
                        resource(json, "battleback2"),
                        integer(json, "battleback2Width", 544),
                        integer(json, "battleback2Height", 416),
                        strings(json, "introPages"),
                        json.has("preemptiveSkillId")
                                ? Math.max(0, json.get("preemptiveSkillId").getAsInt()) : 0
                );
                entries.put(entry.profileId(), entry);
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original battle profile data", exception);
        }
    }

    private static ResourceLocation resource(JsonObject json, String key) {
        String value = json.has(key) ? json.get(key).getAsString() : "";
        return value.isBlank() ? null : new ResourceLocation(value);
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) ? Math.max(1, json.get(key).getAsInt()) : fallback;
    }

    private static List<String> strings(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray(key)) {
            String value = element.getAsString();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    public record Entry(int profileId, int troopId, List<Member> members,
                        ResourceLocation bgm, float bgmVolume, float bgmPitch,
                        ResourceLocation battleback1, int battleback1Width,
                        int battleback1Height, ResourceLocation battleback2,
                        int battleback2Width, int battleback2Height,
                        List<String> introPages, int preemptiveSkillId) {
    }

    public record Member(int profileId, List<Integer> states) {
    }
}
