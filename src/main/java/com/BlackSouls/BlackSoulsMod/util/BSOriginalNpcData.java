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

public final class BSOriginalNpcData {
    private static final Map<String, Entry> ENTRIES = loadEntries();
    private static final Map<Integer, List<Command>> COMMON_EVENTS = loadCommonEvents();

    private BSOriginalNpcData() {
    }

    public static Entry get(String id) {
        return ENTRIES.get(id);
    }

    public static List<Entry> values() {
        return List.copyOf(ENTRIES.values());
    }

    public static List<Command> commonEvent(int id) {
        return COMMON_EVENTS.get(id);
    }

    private static Map<Integer, List<Command>> loadCommonEvents() {
        String path = "/data/blacksouls/original_npc_common_events.json";
        try (InputStream stream = BSOriginalNpcData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original NPC common events: " + path);
            }
            Map<Integer, List<Command>> events = new LinkedHashMap<>();
            for (JsonElement element : JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject json = element.getAsJsonObject();
                List<Command> commands = new ArrayList<>();
                for (JsonElement commandElement : json.getAsJsonArray("commands")) {
                    JsonObject command = commandElement.getAsJsonObject();
                    List<JsonElement> parameters = new ArrayList<>();
                    command.getAsJsonArray("parameters").forEach(parameters::add);
                    commands.add(new Command(command.get("code").getAsInt(),
                            command.get("indent").getAsInt(), parameters));
                }
                events.put(json.get("id").getAsInt(), List.copyOf(commands));
            }
            return Map.copyOf(events);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC common events", exception);
        }
    }

    private static Map<String, Entry> loadEntries() {
        String path = "/data/blacksouls/original_npc_events.json";
        try (InputStream stream = BSOriginalNpcData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original NPC data: " + path);
            }
            Map<String, Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject json = element.getAsJsonObject();
                List<Page> pages = new ArrayList<>();
                for (JsonElement pageElement : json.getAsJsonArray("pages")) {
                    JsonObject page = pageElement.getAsJsonObject();
                    List<Command> commands = new ArrayList<>();
                    for (JsonElement commandElement : page.getAsJsonArray("commands")) {
                        JsonObject command = commandElement.getAsJsonObject();
                        List<JsonElement> parameters = new ArrayList<>();
                        command.getAsJsonArray("parameters").forEach(parameters::add);
                        commands.add(new Command(command.get("code").getAsInt(),
                                command.get("indent").getAsInt(), parameters));
                    }
                    JsonObject movement = new JsonObject();
                    for (String key : List.of("move_speed", "move_frequency", "walk_anime", "step_anime", "direction_fix", "through", "priority_type")) {
                        if (page.has(key)) movement.add(key, page.get(key).deepCopy());
                    }
                    pages.add(new Page(page.getAsJsonObject("condition"), page.getAsJsonObject("graphic"),
                            page.get("trigger").getAsInt(), commands, movement));
                }
                Entry entry = new Entry(json.get("id").getAsString(), json.get("mapId").getAsInt(),
                        json.get("eventId").getAsInt(), json.get("name").getAsString(),
                        json.get("x").getAsInt(), json.get("y").getAsInt(), pages);
                if (entries.put(entry.id(), entry) != null) {
                    throw new IllegalStateException("Duplicate original NPC id: " + entry.id());
                }
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC data", exception);
        }
    }

    public record Entry(String id, int mapId, int eventId, String name, int x, int y, List<Page> pages) {
        public Entry {
            pages = List.copyOf(pages);
        }
    }

    public record Page(JsonObject condition, JsonObject graphic, int trigger, List<Command> commands,
                       JsonObject movement) {
        public Page(JsonObject condition, JsonObject graphic, int trigger, List<Command> commands) {
            this(condition, graphic, trigger, commands, new JsonObject());
        }

        public Page {
            condition = condition.deepCopy();
            graphic = graphic.deepCopy();
            commands = List.copyOf(commands);
            movement = movement.deepCopy();
        }

        @Override
        public JsonObject condition() {
            return condition.deepCopy();
        }

        @Override
        public JsonObject graphic() {
            return graphic.deepCopy();
        }

        @Override
        public JsonObject movement() {
            return movement.deepCopy();
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
