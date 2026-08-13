package com.BlackSouls.BlackSoulsMod.compat.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public final class SceneSpawnerBossData extends SavedData {
    private static final String DATA_NAME = "blacksouls_scene_spawner_bosses";
    private final Set<String> defeatedSpawners = new HashSet<>();
    private int storyProgress;

    public static SceneSpawnerBossData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SceneSpawnerBossData::load,
                SceneSpawnerBossData::new,
                DATA_NAME);
    }

    public static SceneSpawnerBossData load(CompoundTag tag) {
        SceneSpawnerBossData data = new SceneSpawnerBossData();
        ListTag defeated = tag.getList("Defeated", Tag.TAG_STRING);
        for (int index = 0; index < defeated.size(); index++) {
            data.defeatedSpawners.add(defeated.getString(index));
        }
        data.storyProgress = Math.max(0, tag.getInt("StoryProgress"));
        return data;
    }

    public boolean isDefeated(String spawnerKey) {
        return defeatedSpawners.contains(spawnerKey);
    }

    public boolean markDefeated(String spawnerKey) {
        if (!defeatedSpawners.add(spawnerKey)) {
            return false;
        }
        setDirty();
        return true;
    }

    public int getStoryProgress() {
        return storyProgress;
    }

    public int addStoryProgress(int amount) {
        if (amount > 0) {
            storyProgress += amount;
            setDirty();
        }
        return storyProgress;
    }

    public void clear(String spawnerKey) {
        if (defeatedSpawners.remove(spawnerKey)) {
            setDirty();
        }
    }

    public static String spawnerKey(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "@" + pos.asLong();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag defeated = new ListTag();
        defeatedSpawners.stream().sorted().map(StringTag::valueOf).forEach(defeated::add);
        tag.put("Defeated", defeated);
        tag.putInt("StoryProgress", storyProgress);
        return tag;
    }
}
