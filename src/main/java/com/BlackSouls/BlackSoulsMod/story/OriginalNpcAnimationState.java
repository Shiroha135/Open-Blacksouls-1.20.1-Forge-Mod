package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.util.BSOriginalAnimationData;
import com.google.gson.JsonObject;

import java.util.function.IntFunction;

public final class OriginalNpcAnimationState {
    private int requestedId;
    private int activeId;
    private int frameMax;
    private int position;
    private int remainingFrames;
    private int elapsedFrames;

    public void request(int animationId) {
        requestedId = animationId;
    }

    public Change advanceFrame(IntFunction<BSOriginalAnimationData.Entry> metadata) {
        if (activeId > 0) {
            remainingFrames--;
            elapsedFrames++;
            if (remainingFrames == 0) {
                requestedId = 0;
                activeId = 0;
                frameMax = 0;
                position = 0;
                elapsedFrames = 0;
                return Change.ENDED;
            }
            return Change.NONE;
        }
        if (requestedId > 0) {
            BSOriginalAnimationData.Entry animation = metadata.apply(requestedId);
            validate(animation, requestedId);
            activeId = requestedId;
            frameMax = animation.frameMax();
            position = animation.position();
            remainingFrames = Math.addExact(Math.multiplyExact(frameMax, BSOriginalAnimationData.SOURCE_RATE), 1);
            elapsedFrames = 0;
            return Change.STARTED;
        }
        return Change.NONE;
    }

    public int requestedId() {
        return requestedId;
    }

    public int activeId() {
        return activeId;
    }

    public int position() {
        return position;
    }

    public int elapsedFrames() {
        return elapsedFrames;
    }

    public int remainingFrames() {
        return remainingFrames;
    }

    public boolean isWaitComplete() {
        return requestedId <= 0;
    }

    public boolean hasWork() {
        return requestedId > 0 || activeId > 0;
    }

    public void validateActive(IntFunction<BSOriginalAnimationData.Entry> metadata) {
        if (activeId <= 0) return;
        BSOriginalAnimationData.Entry animation = metadata.apply(activeId);
        validate(animation, activeId);
        if (animation.frameMax() != frameMax || animation.position() != position) {
            throw new UnsupportedOperationException("animation_metadata_changed:" + activeId);
        }
    }

    public JsonObject snapshot() {
        JsonObject result = new JsonObject();
        result.addProperty("version", 1);
        result.addProperty("requestedId", requestedId);
        result.addProperty("activeId", activeId);
        result.addProperty("frameMax", frameMax);
        result.addProperty("position", position);
        result.addProperty("remainingFrames", remainingFrames);
        result.addProperty("elapsedFrames", elapsedFrames);
        return result;
    }

    public static OriginalNpcAnimationState restore(JsonObject snapshot) {
        if (integer(snapshot, "version") != 1) throw new IllegalArgumentException("Unsupported NPC animation snapshot");
        OriginalNpcAnimationState state = new OriginalNpcAnimationState();
        state.requestedId = integer(snapshot, "requestedId");
        state.activeId = integer(snapshot, "activeId");
        state.frameMax = integer(snapshot, "frameMax");
        state.position = integer(snapshot, "position");
        state.remainingFrames = integer(snapshot, "remainingFrames");
        state.elapsedFrames = integer(snapshot, "elapsedFrames");
        if (state.activeId < 0 || state.position < 0 || state.position > 3 || state.elapsedFrames < 0
                || state.activeId == 0 && (state.frameMax != 0 || state.remainingFrames != 0 || state.elapsedFrames != 0)
                || state.activeId > 0 && (state.frameMax <= 0 || state.remainingFrames <= 0
                || (long) state.remainingFrames + state.elapsedFrames
                != (long) state.frameMax * BSOriginalAnimationData.SOURCE_RATE + 1L)) {
            throw new IllegalArgumentException("Invalid NPC animation snapshot state");
        }
        return state;
    }

    public static void validate(BSOriginalAnimationData.Entry animation, int id) {
        if (animation == null || animation.id() != id || animation.frameMax() <= 0
                || animation.position() < 0 || animation.position() > 3
                || (long) animation.frameMax() * BSOriginalAnimationData.SOURCE_RATE + 1L > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("animation:" + id);
        }
    }

    private static int integer(JsonObject object, String key) {
        return object.get(key).getAsBigDecimal().intValueExact();
    }

    public enum Change {
        NONE, STARTED, ENDED
    }
}
