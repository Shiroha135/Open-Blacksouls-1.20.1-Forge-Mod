package com.BlackSouls.BlackSoulsMod.util;

public enum DoorConfigMode {
    NONE,
    NORMAL_LOCK,
    STORY_LOCK,
    ANIMATED_GROUP,
    SHORTCUT_GATE,
    SHORTCUT_UNLOCK;

    public DoorConfigMode next() {
        DoorConfigMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
