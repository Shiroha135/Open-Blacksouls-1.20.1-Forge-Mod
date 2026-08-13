package com.BlackSouls.BlackSoulsMod.capability;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class DoorEventProgressSavedData extends SavedData {
    private static final String DATA_NAME = BlackSouls.MODID + "_door_event_progress";
    private final Set<String> triggeredEvents = new HashSet<>();

    public boolean isTriggered(String conditionId) {
        return triggeredEvents.contains(conditionId);
    }

    public void setTriggered(String conditionId, boolean triggered) {
        boolean changed = triggered
                ? triggeredEvents.add(conditionId)
                : triggeredEvents.remove(conditionId);
        if (changed) {
            setDirty();
        }
    }

    public static DoorEventProgressSavedData load(CompoundTag tag) {
        DoorEventProgressSavedData data = new DoorEventProgressSavedData();
        ListTag list = tag.getList("TriggeredEvents", Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            String eventId = list.getString(index).strip();
            if (!eventId.isEmpty()) {
                data.triggeredEvents.add(eventId);
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        triggeredEvents.stream().sorted().map(StringTag::valueOf).forEach(list::add);
        tag.put("TriggeredEvents", list);
        return tag;
    }

    public static DoorEventProgressSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DoorEventProgressSavedData::load,
                DoorEventProgressSavedData::new,
                DATA_NAME
        );
    }
}
