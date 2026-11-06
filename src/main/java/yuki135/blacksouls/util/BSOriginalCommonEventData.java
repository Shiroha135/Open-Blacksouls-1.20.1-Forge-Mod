package yuki135.blacksouls.util;

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

public final class BSOriginalCommonEventData {
    private static final Map<Integer, Entry> ENTRIES = loadEntries();

    private BSOriginalCommonEventData() {
    }

    public static Entry get(int eventId) {
        return ENTRIES.get(eventId);
    }

    public static Map<Integer, Entry> values() {
        return ENTRIES;
    }

    private static Map<Integer, Entry> loadEntries() {
        String path = "/data/blacksouls/original_enemy_common_events.json";
        try (InputStream stream = BSOriginalCommonEventData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original common event data: " + path);
            }
            Map<Integer, Entry> entries = new LinkedHashMap<>();
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
                Entry entry = new Entry(json.get("id").getAsInt(), json.get("name").getAsString(),
                        json.get("trigger").getAsInt(), json.get("switchId").getAsInt(), commands);
                entries.put(entry.id(), entry);
            }
            return Map.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original common event data", exception);
        }
    }

    public record Entry(int id, String name, int trigger, int switchId, List<Command> commands) {
        public Entry {
            commands = List.copyOf(commands);
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
