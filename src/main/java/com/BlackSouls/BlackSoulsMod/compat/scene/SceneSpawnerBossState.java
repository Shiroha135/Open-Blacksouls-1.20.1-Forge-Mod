package com.BlackSouls.BlackSoulsMod.compat.scene;

public interface SceneSpawnerBossState {
    String ENTITY_BOSS_TAG = "BlackSoulsSceneBoss";
    String ENTITY_SCENE_ID_TAG = "BlackSoulsSceneId";
    String ENTITY_SPAWNER_KEY_TAG = "Blacksouls2SceneSpawner";

    boolean blacksouls$isBossMode();

    String blacksouls$getSceneId();

    void blacksouls$setBossMode(boolean bossMode);
}
