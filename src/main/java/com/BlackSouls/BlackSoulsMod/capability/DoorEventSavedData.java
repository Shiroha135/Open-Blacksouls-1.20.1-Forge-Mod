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

public final class DoorEventSavedData extends SavedData {
    private static final String DATA_NAME = BlackSouls.MODID + "_door_events";
    private final Map<Long, DoorEvent> events = new HashMap<>();

    public DoorEvent getEvent(BlockPos pos) {
        return events.get(pos.asLong());
    }

    public void setEvent(BlockPos pos, DoorEvent event) {
        events.put(pos.asLong(), event);
        setDirty();
    }

    public boolean removeEvent(BlockPos pos) {
        if (events.remove(pos.asLong()) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public static DoorEventSavedData load(CompoundTag tag) {
        DoorEventSavedData data = new DoorEventSavedData();
        ListTag eventList = tag.getList("Events", Tag.TAG_COMPOUND);
        for (int index = 0; index < eventList.size(); index++) {
            CompoundTag eventTag = eventList.getCompound(index);
            EventRole role;
            try {
                role = EventRole.valueOf(eventTag.getString("Role"));
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ResourceLocation dimension = ResourceLocation.tryParse(eventTag.getString("TargetDimension"));
            String eventId = eventTag.getString("EventId").strip();
            String conditionId = eventTag.getString("ConditionId").strip();
            if (dimension == null || eventId.isEmpty() || conditionId.isEmpty()) {
                continue;
            }
            data.events.put(eventTag.getLong("Pos"), new DoorEvent(
                    role,
                    eventId,
                    conditionId,
                    dimension,
                    eventTag.getDouble("TargetX"),
                    eventTag.getDouble("TargetY"),
                    eventTag.getDouble("TargetZ")
            ));
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag eventList = new ListTag();
        events.forEach((pos, event) -> {
            CompoundTag eventTag = new CompoundTag();
            eventTag.putLong("Pos", pos);
            eventTag.putString("Role", event.role().name());
            eventTag.putString("EventId", event.eventId());
            eventTag.putString("ConditionId", event.conditionId());
            eventTag.putString("TargetDimension", event.targetDimension().toString());
            eventTag.putDouble("TargetX", event.targetX());
            eventTag.putDouble("TargetY", event.targetY());
            eventTag.putDouble("TargetZ", event.targetZ());
            eventList.add(eventTag);
        });
        tag.put("Events", eventList);
        return tag;
    }

    public static DoorEventSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                DoorEventSavedData::load,
                DoorEventSavedData::new,
                DATA_NAME
        );
    }

    public enum EventRole {
        SHORTCUT_GATE,
        SHORTCUT_UNLOCK
    }

    public record DoorEvent(
            EventRole role,
            String eventId,
            String conditionId,
            ResourceLocation targetDimension,
            double targetX,
            double targetY,
            double targetZ
    ) {
    }
}
