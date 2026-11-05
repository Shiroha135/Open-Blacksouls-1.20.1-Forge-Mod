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

public final class OriginalNpcLogicData {
    private static final Map<String, Integer> EXPECTED_PAGES = Map.of("map051_ev059", 5, "map051_ev095", 8,
            "map051_ev096", 4, "map051_ev097", 4, "map178_ev007", 1);
    private static final Map<String, BSOriginalNpcData.Entry> ENTRIES = load();

    private OriginalNpcLogicData() {
    }

    public static BSOriginalNpcData.Entry get(String id) {
        return id == null ? null : ENTRIES.get(id);
    }

    private static Map<String, BSOriginalNpcData.Entry> load() {
        String path = "/data/blacksouls/original_npc_logic_events.json";
        try (InputStream input = OriginalNpcLogicData.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Missing original logic event data: " + path);
            Map<String, BSOriginalNpcData.Entry> entries = new LinkedHashMap<>();
            for (JsonElement element : JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject json = element.getAsJsonObject();
                int mapId = integer(json, "mapId");
                int eventId = integer(json, "eventId");
                String id = json.get("id").getAsString();
                if (!EXPECTED_PAGES.containsKey(id) || !id.equals("map%03d_ev%03d".formatted(mapId, eventId))) {
                    throw new IllegalArgumentException("Unreviewed original logic event: " + id);
                }
                List<BSOriginalNpcData.Page> pages = new ArrayList<>();
                for (JsonElement pageElement : json.getAsJsonArray("pages")) {
                    JsonObject page = pageElement.getAsJsonObject();
                    List<BSOriginalNpcData.Command> commands = new ArrayList<>();
                    for (JsonElement commandElement : page.getAsJsonArray("commands")) {
                        JsonObject command = commandElement.getAsJsonObject();
                        List<JsonElement> parameters = new ArrayList<>();
                        command.getAsJsonArray("parameters").forEach(parameters::add);
                        int code = integer(command, "code");
                        int indent = integer(command, "indent");
                        if (code < 0 || indent < 0) throw new IllegalArgumentException("Invalid original logic command");
                        commands.add(new BSOriginalNpcData.Command(code, indent, parameters));
                    }
                    if (commands.isEmpty() || commands.get(commands.size() - 1).code() != 0) {
                        throw new IllegalArgumentException("Missing original logic page terminator: " + id);
                    }
                    JsonObject movement = new JsonObject();
                    for (String key : List.of("move_type", "move_speed", "move_frequency", "move_route",
                            "walk_anime", "step_anime", "direction_fix", "through", "priority_type")) {
                        if (!page.has(key)) throw new IllegalArgumentException("Missing original page setting: " + key);
                        movement.add(key, page.get(key).deepCopy());
                    }
                    pages.add(new BSOriginalNpcData.Page(page.getAsJsonObject("condition"), page.getAsJsonObject("graphic"),
                            integer(page, "trigger"), commands, movement));
                }
                if (pages.size() != EXPECTED_PAGES.get(id)) throw new IllegalArgumentException("Unexpected original logic page count: " + id);
                BSOriginalNpcData.Entry entry = new BSOriginalNpcData.Entry(id, mapId, eventId,
                        json.get("name").getAsString(), integer(json, "x"), integer(json, "y"), pages);
                if (entries.put(id, entry) != null) throw new IllegalArgumentException("Duplicate original logic event: " + id);
            }
            if (entries.size() != EXPECTED_PAGES.size()) throw new IllegalArgumentException("Incomplete original logic event data");
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original logic event data", exception);
        }
    }

    private static int integer(JsonObject object, String key) {
        return object.get(key).getAsBigDecimal().intValueExact();
    }
}
