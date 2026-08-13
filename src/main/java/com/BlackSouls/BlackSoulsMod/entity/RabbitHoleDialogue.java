package com.BlackSouls.BlackSoulsMod.entity;

public final class RabbitHoleDialogue {
    public static String[] keys(EntityRabbitHoleNpc.Role role, boolean lowSen, boolean foodEaten,
                                boolean postBoss) {
        String event = role.id().toLowerCase(java.util.Locale.ROOT);
        if (role == EntityRabbitHoleNpc.Role.EV009) {
            String state = postBoss ? "post_boss" : "pre_boss";
            int lines = lowSen ? 2 : postBoss ? 3 : 2;
            String suffix = lowSen ? ".low_sen_" : ".normal_";
            String[] keys = new String[lines];
            for (int index = 0; index < lines; index++) {
                keys[index] = "dialogue.blacksouls.rabbit_hole." + event + "." + state
                        + suffix + (index + 1);
            }
            return keys;
        }
        if (role == EntityRabbitHoleNpc.Role.EV030) {
            if (lowSen) {
                return new String[]{"dialogue.blacksouls.rabbit_hole.ev030.low_sen"};
            }
            return new String[]{
                    "dialogue.blacksouls.rabbit_hole.ev030.normal_1",
                    "dialogue.blacksouls.rabbit_hole.ev030.normal_2"
            };
        }
        String state = lowSen ? "low_sen" : foodEaten ? "food_eaten" : "normal";
        return new String[]{"dialogue.blacksouls.rabbit_hole." + event + "." + state};
    }

    private RabbitHoleDialogue() {
    }
}
