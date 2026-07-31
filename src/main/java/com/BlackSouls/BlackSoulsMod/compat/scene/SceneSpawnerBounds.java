package com.BlackSouls.BlackSoulsMod.compat.scene;

public interface SceneSpawnerBounds {
    int DEFAULT_RANGE = 8;
    int MAX_RANGE = 128;

    String ORIGIN_X_TAG = "BlackSoulsSceneOriginX";
    String ORIGIN_Y_TAG = "BlackSoulsSceneOriginY";
    String ORIGIN_Z_TAG = "BlackSoulsSceneOriginZ";
    String RANGE_X_TAG = "BlackSoulsSceneRangeX";
    String RANGE_Z_TAG = "BlackSoulsSceneRangeZ";

    int blacksouls$getRangeX();

    int blacksouls$getRangeZ();

    void blacksouls$setBounds(int rangeX, int rangeZ);
}
