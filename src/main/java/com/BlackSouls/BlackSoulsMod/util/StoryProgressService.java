package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossData;
import net.minecraft.server.level.ServerLevel;

public final class StoryProgressService {
    public static int get(ServerLevel level) {
        return SceneSpawnerBossData.get(level.getServer()).getStoryProgress();
    }

    private StoryProgressService() {
    }
}
