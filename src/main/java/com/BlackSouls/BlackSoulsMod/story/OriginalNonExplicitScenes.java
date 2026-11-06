package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OriginalNonExplicitScenes {
    public static final int FADE_COMMAND = 900;
    public static final int FADE_TICKS = 20;
    private static final Set<Integer> SCENES = new HashSet<>();
    private static final Set<List<BSOriginalNpcData.Command>> SCENE_COMMANDS = new HashSet<>();

    static {
        try (var stream = OriginalNonExplicitScenes.class.getResourceAsStream("/data/blacksouls/original_npc_common_events.json")) {
            if (stream == null) throw new IllegalStateException("Missing original common events");
            for (JsonElement element : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                var event = element.getAsJsonObject();
                int id = event.get("id").getAsInt();
                if (!event.get("name").getAsString().startsWith("シーン：") && id != 16) continue;
                SCENES.add(id);
                List<BSOriginalNpcData.Command> commands = new ArrayList<>();
                for (var value : event.getAsJsonArray("commands")) {
                    var command = value.getAsJsonObject();
                    List<JsonElement> parameters = new ArrayList<>();
                    command.getAsJsonArray("parameters").forEach(parameters::add);
                    commands.add(new BSOriginalNpcData.Command(command.get("code").getAsInt(), command.get("indent").getAsInt(), parameters));
                }
                SCENE_COMMANDS.add(List.copyOf(commands));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load omitted scene identities", exception);
        }
    }

    public static boolean isScene(int id) {
        return SCENES.contains(id);
    }

    public static boolean isScene(List<BSOriginalNpcData.Command> commands) {
        return SCENE_COMMANDS.contains(commands);
    }

    public static boolean isOmittedCheckpoint(JsonObject snapshot) {
        try {
            var frames = snapshot.getAsJsonArray("frames");
            if (frames == null || frames.isEmpty()) return false;
            var frame = frames.get(0).getAsJsonObject();
            List<BSOriginalNpcData.Command> commands = new ArrayList<>();
            for (var element : frame.getAsJsonArray("commands")) {
                var command = element.getAsJsonObject();
                commands.add(new BSOriginalNpcData.Command(command.get("code").getAsInt(),
                        command.get("indent").getAsInt(), command.getAsJsonArray("parameters").asList()));
            }
            int position = frame.get("position").getAsInt();
            if (position < 0 || position > commands.size()) return false;
            boolean entireScene = frame.has("omittedScene") && frame.get("omittedScene").getAsBoolean() || isScene(commands);
            boolean[] omitted = omittedCommands(commands, entireScene);
            return position < omitted.length && omitted[position] || position > 0 && omitted[position - 1];
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    public static boolean[] omittedCommands(List<BSOriginalNpcData.Command> commands, boolean entireScene) {
        boolean[] omitted = new boolean[commands.size()];
        if (entireScene) {
            Arrays.fill(omitted, true);
            return omitted;
        }
        for (int start = 0; start < commands.size(); start++) {
            var branch = commands.get(start);
            if (branch.code() != 402 && branch.code() != 403) continue;
            int end = start + 1;
            while (end < commands.size()) {
                var command = commands.get(end);
                if (command.indent() == branch.indent()
                        && (command.code() == 402 || command.code() == 403 || command.code() == 404)) break;
                end++;
            }
            boolean containsScene = false;
            for (int index = start + 1; index < end; index++) {
                var command = commands.get(index);
                if (command.code() == 102) break;
                if (command.code() == 117 && isScene(command.parameters().get(0).getAsInt())) {
                    containsScene = true;
                    break;
                }
            }
            if (containsScene) Arrays.fill(omitted, start + 1, end, true);
        }
        return omitted;
    }

    public static boolean presentation(int code) {
        return switch (code) {
            case 101, 401, 108, 408, 205, 505, 212, 213, 221, 222, 223, 224, 225, 230,
                    231, 232, 235, 241, 242, 243, 244, 245, 246, 249, 250, 251 -> true;
            default -> false;
        };
    }

    private OriginalNonExplicitScenes() {
    }
}
