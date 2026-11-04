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

public final class BSOriginalTroopData {
    private static final Map<Integer, Entry> ENTRIES = loadEntries();

    private BSOriginalTroopData() {
    }

    public static Entry get(int troopId) {
        return ENTRIES.get(troopId);
    }

    public static List<Entry> values() {
        return List.copyOf(ENTRIES.values());
    }

    private static Map<Integer, Entry> loadEntries() {
        String path = "/data/blacksouls/original_troops.json";
        try (InputStream stream = BSOriginalTroopData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original troop data: " + path);
            }
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject json = element.getAsJsonObject();
                List<Member> members = new ArrayList<>();
                for (JsonElement memberElement : json.getAsJsonArray("members")) {
                    JsonObject member = memberElement.getAsJsonObject();
                    members.add(new Member(member.get("enemyId").getAsInt(), member.get("x").getAsInt(),
                            member.get("y").getAsInt(), member.get("hidden").getAsBoolean()));
                }
                List<Page> pages = new ArrayList<>();
                for (JsonElement pageElement : json.getAsJsonArray("pages")) {
                    JsonObject page = pageElement.getAsJsonObject();
                    List<Command> commands = new ArrayList<>();
                    for (JsonElement commandElement : page.getAsJsonArray("commands")) {
                        JsonObject command = commandElement.getAsJsonObject();
                        List<JsonElement> parameters = new ArrayList<>();
                        command.getAsJsonArray("parameters").forEach(parameters::add);
                        commands.add(new Command(command.get("code").getAsInt(), command.get("indent").getAsInt(), parameters));
                    }
                    pages.add(new Page(page.getAsJsonObject("condition"), page.get("span").getAsInt(), commands));
                }
                Entry entry = new Entry(json.get("id").getAsInt(), json.get("name").getAsString(), members, pages);
                if (entries.put(entry.id(), entry) != null) {
                    throw new IllegalStateException("Duplicate original troop id: " + entry.id());
                }
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original troop data", exception);
        }
    }

    public record Entry(int id, String name, List<Member> members, List<Page> pages) {
        public Entry {
            members = List.copyOf(members);
            pages = List.copyOf(pages);
        }
    }

    public record Member(int enemyId, int x, int y, boolean hidden) {
    }

    public record Page(JsonObject condition, int span, List<Command> commands) {
        public Page {
            condition = condition.deepCopy();
            commands = List.copyOf(commands);
        }

        @Override
        public JsonObject condition() {
            return condition.deepCopy();
        }
    }

    public record Command(int code, int indent, List<JsonElement> parameters) {
        public Command {
            parameters = parameters.stream().map(JsonElement::deepCopy).toList();
        }

        @Override
        public List<JsonElement> parameters() {
            return parameters.stream().map(JsonElement::deepCopy).toList();
        }
    }
}
