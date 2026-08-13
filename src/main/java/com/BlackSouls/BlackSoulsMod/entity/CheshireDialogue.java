package com.BlackSouls.BlackSoulsMod.entity;

import java.util.Locale;
import java.util.Map;

public final class CheshireDialogue {
    private static final String PREFIX = "dialogue.blacksouls.cheshire.";
    private static final Map<String, String[]> SCENES = Map.ofEntries(
            Map.entry("map_002", keys("map_002")),
            Map.entry("map_006", keys("map_006")),
            Map.entry("map_009", keys("map_009")),
            Map.entry("map_011", keys("map_011")),
            Map.entry("map_026", keys("map_026")),
            Map.entry("map_031", keys("map_031")),
            Map.entry("map_033", keys("map_033")),
            Map.entry("map_034", keys("map_034")),
            Map.entry("map_035", keys("map_035")),
            Map.entry("map_036", keys("map_036")),
            Map.entry("map_037", keys("map_037")),
            Map.entry("map_038", keys("map_038")),
            Map.entry("map_039", keys("map_039")),
            Map.entry("map_042", keys("map_042")),
            Map.entry("map_078", keys("map_078")),
            Map.entry("map_091", keys("map_091")),
            Map.entry("map_095", keys("map_095")),
            Map.entry("map_123", keys("map_123")),
            Map.entry("map_126", keys("map_126")),
            Map.entry("map_151", keys("map_151")),
            Map.entry("map_152", keys("map_152")),
            Map.entry("map_156", keys("map_156")),
            Map.entry("map_158", keys("map_158")),
            Map.entry("map_172", keys("map_172")),
            Map.entry("map_310", keys("map_310")),
            Map.entry("map_331", keys("map_331")),
            Map.entry("map_343", keys("map_343")),
            Map.entry("map_347", keys("map_347")),
            Map.entry("map_353", keys("map_353"))
    );

    public static String normalizeSceneId(String sceneId) {
        String normalized = sceneId == null ? "" : sceneId.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.lastIndexOf('#');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    public static boolean isRabbitHole(String sceneId) {
        return "map_051".equals(normalizeSceneId(sceneId));
    }

    public static String[] keysFor(String sceneId) {
        String normalized = normalizeSceneId(sceneId);
        if (isRabbitHole(normalized)) {
            return new String[]{PREFIX + "rabbit_hole.intro_1", PREFIX + "rabbit_hole.intro_2"};
        }
        return SCENES.getOrDefault(normalized, keys("generic"));
    }

    public static String[] rabbitHoleAnswerKeys() {
        return new String[]{
                PREFIX + "rabbit_hole.answer_1",
                PREFIX + "rabbit_hole.answer_2",
                PREFIX + "rabbit_hole.answer_3"
        };
    }

    public static String[] rabbitHoleGiftKeys() {
        return new String[]{
                PREFIX + "rabbit_hole.gift_1",
                PREFIX + "rabbit_hole.gift_2"
        };
    }

    public static String[] rabbitHoleAfterGiftKeys() {
        return new String[]{PREFIX + "rabbit_hole.after_gift"};
    }

    public enum Mode {
        NONE,
        INTRO,
        GIFT,
        AFTER_GIFT,
        RABBIT_KNIGHT;

        public static Mode fromNetwork(int value) {
            return value >= 0 && value < values().length ? values()[value] : NONE;
        }
    }

    private static String[] keys(String scene) {
        return new String[]{PREFIX + scene};
    }

    private CheshireDialogue() {
    }
}
