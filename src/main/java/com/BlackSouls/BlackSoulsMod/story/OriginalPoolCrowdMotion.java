package com.BlackSouls.BlackSoulsMod.story;

public final class OriginalPoolCrowdMotion {
    private OriginalPoolCrowdMotion() {
    }

    public static OriginalNpcMotion retimeWalk(OriginalNpcMotion motion, int currentSpeed) {
        if (motion == null) return null;
        var state = motion.snapshot();
        if (state.get("jump").getAsBoolean() || state.get("moveSpeed").getAsInt() == currentSpeed) return motion;
        return OriginalNpcMotion.walk(motion.x(), motion.y(), motion.z(), state.get("targetX").getAsDouble(),
                state.get("targetY").getAsDouble(), state.get("targetZ").getAsDouble(), currentSpeed);
    }
}
