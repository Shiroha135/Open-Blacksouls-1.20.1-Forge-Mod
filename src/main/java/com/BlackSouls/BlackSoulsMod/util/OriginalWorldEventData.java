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

public final class OriginalWorldEventData {
    private static final Map<String, BSOriginalNpcData.Entry> ENTRIES = load();

    private OriginalWorldEventData() {
    }

    public static BSOriginalNpcData.Entry get(String id) {
        return ENTRIES.get(id);
    }

    public static boolean isNativeBlock(String id) {
        return ENTRIES.containsKey(id);
    }

    private static Map<String, BSOriginalNpcData.Entry> load() {
        try (var stream = OriginalWorldEventData.class.getResourceAsStream("/data/blacksouls/original_world_events.json")) {
            if (stream == null) throw new IllegalStateException("Missing original world event data");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, BSOriginalNpcData.Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("events")) {
                JsonObject row = element.getAsJsonObject();
                String id = row.get("id").getAsString();
                int mapId = row.get("mapId").getAsInt(), eventId = row.get("eventId").getAsInt();
                if (!id.equals("map%03d_ev%03d".formatted(mapId, eventId)) || !row.get("role").getAsString().equals("native_block")
                        || BSOriginalNpcData.get(id) != null || OriginalNpcLogicData.get(id) != null) throw new IllegalArgumentException("Invalid original world event identity");
                List<BSOriginalNpcData.Page> pages = new ArrayList<>();
                for (var value : row.getAsJsonArray("pages")) {
                    JsonObject page = value.getAsJsonObject();
                    List<BSOriginalNpcData.Command> commands = new ArrayList<>();
                    for (var raw : page.getAsJsonArray("commands")) {
                        var command = raw.getAsJsonObject();
                        List<JsonElement> parameters = new ArrayList<>();
                        command.getAsJsonArray("parameters").forEach(parameter -> parameters.add(parameter.deepCopy()));
                        commands.add(new BSOriginalNpcData.Command(command.get("code").getAsInt(), command.get("indent").getAsInt(), parameters));
                    }
                    JsonObject movement = new JsonObject();
                    for (String key : List.of("move_type", "move_speed", "move_frequency", "move_route", "walk_anime", "step_anime", "direction_fix", "through", "priority_type")) {
                        if (page.has(key)) movement.add(key, page.get(key).deepCopy());
                    }
                    pages.add(new BSOriginalNpcData.Page(page.getAsJsonObject("condition"), page.getAsJsonObject("graphic"),
                            page.get("trigger").getAsInt(), commands, movement));
                }
                if (pages.isEmpty() || entries.put(id, new BSOriginalNpcData.Entry(id, mapId, eventId, row.get("name").getAsString(),
                        row.get("x").getAsInt(), row.get("y").getAsInt(), pages)) != null) throw new IllegalArgumentException("Duplicate original world event");
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original world events", exception);
        }
    }
}
