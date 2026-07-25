package com.BlackSouls.BlackSoulsMod.capability;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DoorLockSavedData extends SavedData {
    private static final String DATA_NAME = BlackSouls.MODID + "_door_locks";
    private final Map<Long, DoorLock> locks = new HashMap<>();

    public DoorLock getLock(BlockPos pos) {
        return locks.get(pos.asLong());
    }

    public void setLock(BlockPos pos, DoorLock lock) {
        locks.put(pos.asLong(), lock);
        setDirty();
    }

    public boolean removeLock(BlockPos pos) {
        if (locks.remove(pos.asLong()) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public static DoorLockSavedData load(CompoundTag tag) {
        DoorLockSavedData data = new DoorLockSavedData();
        ListTag list = tag.getList("Locks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag lockTag = list.getCompound(i);
            LockType type;
            try {
                type = LockType.valueOf(lockTag.getString("Type"));
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ResourceLocation requiredItem = null;
            if (lockTag.contains("RequiredItem", Tag.TAG_STRING)) {
                requiredItem = ResourceLocation.tryParse(lockTag.getString("RequiredItem"));
            }
            data.locks.put(
                    lockTag.getLong("Pos"),
                    new DoorLock(type, requiredItem, !lockTag.contains("Consume") || lockTag.getBoolean("Consume"))
            );
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        locks.forEach((pos, lock) -> {
            CompoundTag lockTag = new CompoundTag();
            lockTag.putLong("Pos", pos);
            lockTag.putString("Type", lock.type().name());
            if (lock.requiredItem() != null) {
                lockTag.putString("RequiredItem", lock.requiredItem().toString());
            }
            lockTag.putBoolean("Consume", lock.consume());
            list.add(lockTag);
        });
        tag.put("Locks", list);
        return tag;
    }

    public static DoorLockSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                DoorLockSavedData::load,
                DoorLockSavedData::new,
                DATA_NAME
        );
    }

    public enum LockType {
        NORMAL,
        STORY
    }

    public record DoorLock(LockType type, ResourceLocation requiredItem, boolean consume) {
    }
}
