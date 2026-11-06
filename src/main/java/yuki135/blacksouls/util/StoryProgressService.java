package yuki135.blacksouls.util;

import yuki135.blacksouls.compat.scene.SceneSpawnerBossData;
import net.minecraft.server.level.ServerLevel;

public final class StoryProgressService {
    public static int get(ServerLevel level) {
        return SceneSpawnerBossData.get(level.getServer()).getStoryProgress();
    }

    private StoryProgressService() {
    }
}
