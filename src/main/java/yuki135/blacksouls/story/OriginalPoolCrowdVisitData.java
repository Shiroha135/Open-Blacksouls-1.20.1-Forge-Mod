package yuki135.blacksouls.story;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class OriginalPoolCrowdVisitData extends SavedData {
    public static final String DATA_NAME = "blacksouls_original_pool_crowd_visits";
    private long generation;
    private UUID owner;

    public static OriginalPoolCrowdVisitData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OriginalPoolCrowdVisitData::load,
                OriginalPoolCrowdVisitData::new, DATA_NAME);
    }

    public long generation() {
        return generation;
    }

    public UUID owner() {
        return owner;
    }

    public void rebuild(UUID player) {
        if (player == null || generation == Long.MAX_VALUE) throw new IllegalStateException("original_pool_visit_generation");
        generation++;
        owner = player;
        setDirty();
    }

    public static OriginalPoolCrowdVisitData load(CompoundTag tag) {
        var data = new OriginalPoolCrowdVisitData();
        data.generation = Math.max(0, tag.getLong("Generation"));
        data.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("Generation", generation);
        if (owner != null) tag.putUUID("Owner", owner);
        return tag;
    }
}
