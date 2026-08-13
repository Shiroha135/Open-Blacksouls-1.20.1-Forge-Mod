package com.BlackSouls.BlackSoulsMod.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class StoryNameData {
    public static final int MAX_LENGTH = 4;
    public static final int GAME_NAME_MAX_LENGTH = 16;
    private static final String TAG_STARTED = "bs2_story_name_started";
    private static final String TAG_CONFIRMED = "bs2_story_name_confirmed";
    private static final String TAG_NAME = "bs2_story_name";

    public static boolean hasStarted(Player player) {
        return data(player).getBoolean(TAG_STARTED);
    }

    public static boolean isConfirmed(Player player) {
        return data(player).getBoolean(TAG_CONFIRMED);
    }

    public static void start(Player player) {
        CompoundTag data = data(player);
        data.putBoolean(TAG_STARTED, true);
        data.putBoolean(TAG_CONFIRMED, false);
    }

    public static void confirm(Player player, String name) {
        String normalized = normalize(name);
        store(player, normalized);
    }

    public static void confirmGameName(Player player) {
        store(player, normalizeGameName(player.getGameProfile().getName()));
    }

    private static void store(Player player, String normalized) {
        if (normalized.isEmpty()) {
            return;
        }
        CompoundTag data = data(player);
        data.putBoolean(TAG_STARTED, true);
        data.putBoolean(TAG_CONFIRMED, true);
        data.putString(TAG_NAME, normalized);
    }

    public static String get(Player player) {
        String stored = data(player).getString(TAG_NAME);
        return stored.isEmpty() ? player.getGameProfile().getName() : stored;
    }

    public static String normalize(String value) {
        return normalize(value, MAX_LENGTH);
    }

    public static String normalizeGameName(String value) {
        return normalize(value, GAME_NAME_MAX_LENGTH);
    }

    private static String normalize(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String stripped = value.strip();
        StringBuilder result = new StringBuilder();
        stripped.codePoints()
            .filter(codePoint -> !Character.isISOControl(codePoint) && codePoint != 0xA7)
            .limit(maxLength)
            .forEach(result::appendCodePoint);
        return result.toString();
    }

    private static CompoundTag data(Player player) {
        return SkillUtils.getPersistedData(player);
    }

    private StoryNameData() {
    }
}
