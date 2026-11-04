package com.BlackSouls.BlackSoulsMod.compat.scene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OriginalNpcPlacementData extends SavedData {
    public static final String DATA_NAME = "blacksouls_original_npc_placements";
    private final Map<String, UUID> installed = new HashMap<>();

    public static OriginalNpcPlacementData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OriginalNpcPlacementData::load,
                OriginalNpcPlacementData::new, DATA_NAME);
    }

    public boolean isInstalled(String npcId) {
        return installed.containsKey(npcId);
    }

    public UUID entityId(String npcId) {
        return installed.get(npcId);
    }

    public boolean record(String npcId, UUID entityId) {
        if (npcId == null || npcId.isBlank() || entityId == null) {
            throw new IllegalArgumentException("Invalid original NPC placement record");
        }
        if (installed.putIfAbsent(npcId, entityId) != null) {
            return false;
        }
        setDirty();
        return true;
    }

    public static OriginalNpcPlacementData load(CompoundTag tag) {
        OriginalNpcPlacementData data = new OriginalNpcPlacementData();
        for (Tag value : tag.getList("Installed", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            String npcId = entry.getString("NpcId");
            if (!npcId.isBlank() && entry.hasUUID("EntityId")) {
                data.installed.putIfAbsent(npcId, entry.getUUID("EntityId"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        installed.keySet().stream().sorted().forEach(npcId -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("NpcId", npcId);
            entry.putUUID("EntityId", installed.get(npcId));
            entries.add(entry);
        });
        tag.put("Installed", entries);
        return tag;
    }
}
