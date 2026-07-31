package com.BlackSouls.BlackSoulsMod.entity;

public final class RabbitHoleDialogue {
    public static String[] keys(EntityRabbitHoleNpc.Role role, boolean lowSen, boolean foodEaten) {
        String event = role.id().toLowerCase(java.util.Locale.ROOT);
        String state = lowSen ? "low_sen" : foodEaten ? "food_eaten" : "normal";
        return new String[]{"dialogue.blacksouls.rabbit_hole." + event + "." + state};
    }

    private RabbitHoleDialogue() {
    }
}
