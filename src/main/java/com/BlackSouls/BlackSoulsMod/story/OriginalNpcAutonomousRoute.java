package com.BlackSouls.BlackSoulsMod.story;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Set;

public final class OriginalNpcAutonomousRoute {
    private OriginalNpcMoveRouteRunner runner;
    private int stopCount;
    private boolean locked;
    private int prelockDirection;
    private String failure = "";
    private boolean validated;
    private boolean forcedSuspended;
    private boolean pastEnd;
    OriginalNpcMotion motion;
    boolean gravityCaptured;
    boolean originalNoGravity;

    public void configure(JsonObject movement) {
        runner = null;
        validated = false;
        failure = "";
        forcedSuspended = false;
        pastEnd = false;
        if (!movement.has("move_type") || movement.get("move_type").getAsInt() != 3) return;
        try {
            runner = new OriginalNpcMoveRouteRunner(movement.getAsJsonObject("move_route"));
        } catch (RuntimeException exception) {
            failure = exception.getClass().getSimpleName() + ":" + exception.getMessage();
        }
    }

    public void advanceFrame(Context context) {
        context.animate(stopCount);
        if (context.advanceMovementFrame()) return;
        advanceStopFrame();
        advanceAfterForcedFrame(context);
    }

    public void advanceAfterForcedFrame(Context context) {
        if (locked || runner == null || !failure.isEmpty() || !context.nearScreen()
                || pastEnd || stopCount <= 30 * (5 - context.moveFrequency())) return;
        try {
            if (!validated) {
                runner.validate(context);
                validated = true;
            }
            runner.advanceFrame(new OriginalNpcMoveRouteRunner.Context() {
                @Override
                public void validate(int code, List<JsonElement> parameters) {
                    context.validate(code, parameters);
                }

                @Override
                public void apply(int code, List<JsonElement> parameters) {
                    context.apply(code, parameters);
                    if (code >= 16 && code <= 26 || code >= 1 && code <= 14 && context.movementSucceeded()) resetStopCount();
                }

                @Override
                public boolean movementSucceeded() {
                    return context.movementSucceeded();
                }
            });
        } catch (RuntimeException exception) {
            failure = exception.getClass().getSimpleName() + ":" + exception.getMessage();
        }
    }

    public void advanceStopFrame() {
        if (!locked && stopCount < Integer.MAX_VALUE) stopCount++;
    }

    public void resetStopCount() {
        stopCount = 0;
    }

    public int stopCount() {
        return stopCount;
    }

    public boolean isLocked() {
        return locked;
    }

    public String failure() {
        return failure;
    }

    public boolean lock(int direction) {
        if (locked) return false;
        require(direction == 2 || direction == 4 || direction == 6 || direction == 8, "lock_direction");
        prelockDirection = direction;
        locked = true;
        resetStopCount();
        return true;
    }

    public int unlock() {
        if (!locked) return 0;
        locked = false;
        resetStopCount();
        int direction = prelockDirection;
        prelockDirection = 0;
        return direction;
    }

    public void beforeForcedRoute() {
        prelockDirection = 0;
        forcedSuspended = true;
        if (runner != null) {
            JsonObject state = runner.snapshot();
            state.addProperty("waitFrames", 0);
            runner = OriginalNpcMoveRouteRunner.restore(state);
        }
    }

    public void afterForcedRoute() {
        if (!forcedSuspended) return;
        forcedSuspended = false;
        if (runner == null || pastEnd) return;
        JsonObject state = runner.snapshot();
        int position = state.get("position").getAsInt() + 1;
        if (position >= state.getAsJsonObject("route").getAsJsonArray("list").size()) {
            pastEnd = true;
        } else {
            state.addProperty("position", position);
            runner = OriginalNpcMoveRouteRunner.restore(state);
        }
    }

    public void clearPrelockDirection() {
        prelockDirection = 0;
    }

    public JsonObject snapshot() {
        JsonObject state = new JsonObject();
        state.addProperty("version", 1);
        state.addProperty("stopCount", stopCount);
        state.addProperty("locked", locked);
        state.addProperty("prelockDirection", prelockDirection);
        state.addProperty("failure", failure);
        state.addProperty("gravityCaptured", gravityCaptured);
        state.addProperty("originalNoGravity", originalNoGravity);
        state.addProperty("forcedSuspended", forcedSuspended);
        state.addProperty("pastEnd", pastEnd);
        if (runner != null) state.add("runner", runner.snapshot());
        if (motion != null) state.add("motion", motion.snapshot());
        return state;
    }

    public void restore(JsonObject state) {
        Set<String> required = Set.of("version", "stopCount", "locked", "prelockDirection", "failure", "gravityCaptured", "originalNoGravity", "forcedSuspended", "pastEnd");
        require(state.keySet().containsAll(required) && state.keySet().stream().allMatch(key -> required.contains(key)
                || key.equals("runner") || key.equals("motion")), "state_fields");
        require(integer(state, "version") == 1 && integer(state, "stopCount") >= 0, "state_version");
        int direction = integer(state, "prelockDirection");
        require(direction == 0 || direction == 2 || direction == 4 || direction == 6 || direction == 8, "prelock_direction");
        boolean restoredLocked = bool(state, "locked");
        require(restoredLocked || direction == 0, "unlocked_direction");
        require(state.has("runner") == (runner != null), "runner_page");
        OriginalNpcMoveRouteRunner restored = null;
        if (runner != null) {
            restored = OriginalNpcMoveRouteRunner.restore(state.getAsJsonObject("runner"));
            require(restored.snapshot().get("route").equals(runner.snapshot().get("route")), "runner_source");
        }
        require(state.get("failure").isJsonPrimitive() && state.get("failure").getAsJsonPrimitive().isString(), "failure");
        boolean captured = bool(state, "gravityCaptured");
        OriginalNpcMotion restoredMotion = state.has("motion") ? OriginalNpcMotion.restore(state.getAsJsonObject("motion")) : null;
        require(captured == (restoredMotion != null) && (restoredMotion == null || !restoredMotion.complete()), "motion_phase");
        boolean restoredOriginalNoGravity = bool(state, "originalNoGravity");
        boolean restoredSuspended = bool(state, "forcedSuspended");
        boolean restoredPastEnd = bool(state, "pastEnd");
        require(!restoredPastEnd || restored != null, "past_end");
        runner = restored;
        stopCount = integer(state, "stopCount");
        locked = restoredLocked;
        prelockDirection = direction;
        failure = state.get("failure").getAsString();
        gravityCaptured = captured;
        originalNoGravity = restoredOriginalNoGravity;
        forcedSuspended = restoredSuspended;
        pastEnd = restoredPastEnd;
        motion = restoredMotion;
        validated = false;
    }

    private static int integer(JsonObject state, String name) {
        JsonElement value = state.get(name);
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), name);
        return value.getAsBigDecimal().intValueExact();
    }

    private static boolean bool(JsonObject state, String name) {
        JsonElement value = state.get(name);
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean(), name);
        return value.getAsBoolean();
    }

    private static void require(boolean condition, String name) {
        if (!condition) throw new IllegalArgumentException("Invalid autonomous route:" + name);
    }

    public interface Context extends OriginalNpcMoveRouteRunner.Context {
        boolean nearScreen();
        int moveFrequency();
        void animate(int stopCount);
    }
}
