package com.BlackSouls.BlackSoulsMod.entity;

public final class RabbitKnightDialogue {
    private static final String PREFIX = "dialogue.blacksouls.rabbit_knight.";

    public static String[] introductionKeys() {
        return keys("intro", 4);
    }

    public static String[] repeatKeys() {
        return new String[]{PREFIX + "repeat"};
    }

    public static String[] aliceAnswerKeys() {
        return keys("alice", 6);
    }

    public static String deathLine() {
        return "「畜生啊……到头来我也……和老爹一个样么……」";
    }

    private static String[] keys(String group, int count) {
        String[] result = new String[count];
        for (int index = 0; index < count; index++) {
            result[index] = PREFIX + group + "_" + (index + 1);
        }
        return result;
    }

    private RabbitKnightDialogue() {
    }
}
