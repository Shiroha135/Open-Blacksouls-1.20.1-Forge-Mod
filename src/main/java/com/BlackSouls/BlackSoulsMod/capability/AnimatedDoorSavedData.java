package com.BlackSouls.BlackSoulsMod.capability;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public final class AnimatedDoorSavedData extends SavedData {
    private static final String DATA_NAME = BlackSouls.MODID + "_animated_doors";
    private final Set<Long> doors = new HashSet<>();

    public boolean contains(BlockPos pos) {
        return doors.contains(pos.asLong());
    }

    public void set(BlockPos pos, boolean enabled) {
        boolean changed = enabled ? doors.add(pos.asLong()) : doors.remove(pos.asLong());
        if (changed) {
            setDirty();
        }
    }

    public boolean toggle(BlockPos pos) {
        long key = pos.asLong();
        boolean added;
        if (doors.remove(key)) {
            added = false;
        } else {
            doors.add(key);
            added = true;
        }
        setDirty();
        return added;
    }

    public Set<Long> positions() {
        return Set.copyOf(doors);
    }

    public void remove(long pos) {
        if (doors.remove(pos)) {
            setDirty();
        }
    }

    public static AnimatedDoorSavedData load(CompoundTag tag) {
        AnimatedDoorSavedData data = new AnimatedDoorSavedData();
        ListTag list = tag.getList("Doors", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            data.doors.add(list.getCompound(index).getLong("Pos"));
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (long pos : doors) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos);
            list.add(entry);
        }
        tag.put("Doors", list);
        return tag;
    }

    public static AnimatedDoorSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                AnimatedDoorSavedData::load,
                AnimatedDoorSavedData::new,
                DATA_NAME
        );
    }
}
