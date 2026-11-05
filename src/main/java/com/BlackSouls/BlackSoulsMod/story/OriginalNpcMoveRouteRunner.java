package com.BlackSouls.BlackSoulsMod.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class OriginalNpcMoveRouteRunner {
    private final JsonObject route;
    private final List<Command> commands;
    private final boolean repeat;
    private final boolean wait;
    private final boolean skippable;
    private int position;
    private int waitFrames;
    private long elapsedFrames;
    private boolean complete;

    public OriginalNpcMoveRouteRunner(JsonObject source) {
        route = source.deepCopy();
        repeat = bool(route, "repeat");
        wait = bool(route, "wait");
        skippable = bool(route, "skippable");
        require(route.has("list") && route.get("list").isJsonArray(), "missing_list");
        JsonArray list = route.getAsJsonArray("list");
        require(!list.isEmpty() && list.size() <= 4096, "list_size");
        List<Command> parsed = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            try {
                JsonObject command = list.get(index).getAsJsonObject();
                int code = integer(command.get("code"), 0, 45);
                JsonArray parameters = command.getAsJsonArray("parameters");
                require(parameters != null, "missing_parameters");
                List<JsonElement> values = new ArrayList<>();
                parameters.forEach(value -> values.add(value.deepCopy()));
                validate(code, values);
                require(code != 0 || index == list.size() - 1, "embedded_route_end");
                parsed.add(new Command(code, values));
            } catch (RuntimeException exception) {
                throw new UnsupportedOperationException("route_command:" + index + ":" + exception.getMessage(), exception);
            }
        }
        require(parsed.get(parsed.size() - 1).code() == 0, "missing_route_end");
        commands = List.copyOf(parsed);
    }

    public void validate(Context context) {
        for (int index = 0; index < commands.size(); index++) {
            Command command = commands.get(index);
            try {
                context.validate(command.code(), command.parameters());
            } catch (RuntimeException exception) {
                throw new UnsupportedOperationException("route_command:" + index + ":code:"
                        + command.code() + ":" + exception.getMessage(), exception);
            }
        }
    }

    public void advanceFrame(Context context) {
        if (complete) return;
        elapsedFrames = Math.addExact(elapsedFrames, 1L);
        if (context.advanceMovementFrame()) return;
        if (waitFrames > 0) {
            waitFrames--;
            return;
        }
        Command command = commands.get(position);
        if (command.code() == 0) {
            if (repeat) position = 0;
            else complete = true;
        } else {
            if (command.code() == 15) {
                waitFrames = Math.max(0, command.parameters().get(0).getAsInt() - 1);
            } else {
                context.apply(command.code(), command.parameters());
            }
            if (command.code() > 14 || context.movementSucceeded() || skippable) position++;
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean waitsForCompletion() {
        return wait;
    }

    public long elapsedFrames() {
        return elapsedFrames;
    }

    public JsonObject snapshot() {
        JsonObject value = new JsonObject();
        value.addProperty("version", 1);
        value.add("route", route.deepCopy());
        value.addProperty("position", position);
        value.addProperty("waitFrames", waitFrames);
        value.addProperty("elapsedFrames", elapsedFrames);
        value.addProperty("complete", complete);
        return value;
    }

    public static OriginalNpcMoveRouteRunner restore(JsonObject snapshot) {
        require(integer(snapshot.get("version"), 1, 1) == 1, "snapshot_version");
        OriginalNpcMoveRouteRunner runner = new OriginalNpcMoveRouteRunner(snapshot.getAsJsonObject("route"));
        runner.position = integer(snapshot.get("position"), 0, runner.commands.size() - 1);
        runner.waitFrames = integer(snapshot.get("waitFrames"), 0, 360000);
        runner.elapsedFrames = snapshot.get("elapsedFrames").getAsBigDecimal().longValueExact();
        runner.complete = bool(snapshot, "complete");
        require(runner.elapsedFrames >= 0L && (!runner.complete || !runner.repeat
                && runner.position == runner.commands.size() - 1 && runner.waitFrames == 0), "snapshot_state");
        return runner;
    }

    public static boolean waitRequested(JsonObject route) {
        return bool(route, "wait");
    }

    private static void validate(int code, List<JsonElement> parameters) {
        switch (code) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40 -> size(parameters, 0);
            case 14 -> {
                size(parameters, 2);
                integer(parameters.get(0), -1000000, 1000000);
                integer(parameters.get(1), -1000000, 1000000);
            }
            case 15, 27, 28, 29, 30, 42, 43 -> {
                size(parameters, 1);
                int minimum = code == 27 || code == 28 || code == 29 || code == 30 ? 1 : 0;
                int maximum = switch (code) {
                    case 15 -> 360000;
                    case 29 -> 6;
                    case 30 -> 5;
                    case 42 -> 255;
                    case 43 -> 0;
                    default -> Integer.MAX_VALUE;
                };
                integer(parameters.get(0), minimum, maximum);
            }
            case 41 -> {
                size(parameters, 2);
                require(parameters.get(0).isJsonPrimitive()
                        && parameters.get(0).getAsJsonPrimitive().isString(), "graphic_name");
                integer(parameters.get(1), 0, 7);
            }
            case 44 -> {
                size(parameters, 1);
                require(parameters.get(0).isJsonObject(), "sound");
            }
            default -> throw new UnsupportedOperationException("unsupported_route_code:" + code);
        }
    }

    private static int integer(JsonElement element, int minimum, int maximum) {
        require(element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber(), "integer");
        int value = element.getAsBigDecimal().intValueExact();
        require(value >= minimum && value <= maximum, "integer_range:" + value);
        return value;
    }

    private static boolean bool(JsonObject object, String key) {
        require(object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isBoolean(), "boolean:" + key);
        return object.get(key).getAsBoolean();
    }

    private static void size(List<JsonElement> parameters, int count) {
        require(parameters.size() == count, "parameter_count:" + parameters.size());
    }

    private static void require(boolean value, String reason) {
        if (!value) throw new UnsupportedOperationException(reason);
    }

    private record Command(int code, List<JsonElement> parameters) {
        private Command {
            parameters = parameters.stream().map(JsonElement::deepCopy).toList();
        }

        @Override
        public List<JsonElement> parameters() {
            return parameters.stream().map(JsonElement::deepCopy).toList();
        }
    }

    public interface Context {
        void validate(int code, List<JsonElement> parameters);
        void apply(int code, List<JsonElement> parameters);

        default boolean advanceMovementFrame() {
            return false;
        }

        default boolean movementSucceeded() {
            return true;
        }
    }
}
