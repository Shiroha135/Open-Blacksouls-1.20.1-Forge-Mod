package com.BlackSouls.BlackSoulsMod.compat.scene;

public final class Map052BossSpawnerRepair {
    public static final String TARGET_SCENE = "map_052";

    private Map052BossSpawnerRepair() {
    }

    public static boolean shouldRepair(String dimension, int x, int y, int z, String sceneId,
                                       String templateItemId, boolean originalTemplate) {
        return "blacksouls:hokoniwa".equals(dimension) && x == 132 && y == -19 && z == 213
                && "map_051".equals(sceneId) && originalTemplate
                && "blacksouls:original_enemy_185_spawn_egg".equals(templateItemId);
    }

    public interface Access {
        void blacksouls$repairOriginalBossScene();
    }
}
