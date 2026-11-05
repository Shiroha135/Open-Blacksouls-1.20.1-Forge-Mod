package com.BlackSouls.BlackSoulsMod.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OriginalNpcShopStorageData extends SavedData {
    private final Map<UUID, Map<ItemKey, Integer>> inventories = new HashMap<>();

    public static OriginalNpcShopStorageData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OriginalNpcShopStorageData::load,
                OriginalNpcShopStorageData::new, "blacksouls_original_npc_shop_storage");
    }

    public int count(UUID owner, int kind, int id) {
        return inventories.getOrDefault(owner, Map.of()).getOrDefault(new ItemKey(kind, id), 0);
    }

    public Map<ItemKey, Integer> contents(UUID owner) {
        return Map.copyOf(inventories.getOrDefault(owner, Map.of()));
    }

    public void setCount(UUID owner, int kind, int id, int count) {
        if (owner == null || kind < 0 || kind > 2 || id < 1 || count < 0 || count > 999) {
            throw new IllegalArgumentException("Invalid original shop storage entry");
        }
        ItemKey key = new ItemKey(kind, id);
        if (count(owner, kind, id) == count) return;
        if (count == 0) {
            Map<ItemKey, Integer> inventory = inventories.get(owner);
            if (inventory != null) {
                inventory.remove(key);
                if (inventory.isEmpty()) inventories.remove(owner);
            }
        } else {
            inventories.computeIfAbsent(owner, ignored -> new HashMap<>()).put(key, count);
        }
        setDirty();
    }

    public static OriginalNpcShopStorageData load(CompoundTag tag) {
        OriginalNpcShopStorageData data = new OriginalNpcShopStorageData();
        CompoundTag owners = tag.getCompound("Owners");
        for (String name : owners.getAllKeys()) {
            UUID owner = UUID.fromString(name);
            CompoundTag inventory = owners.getCompound(name);
            Map<ItemKey, Integer> entries = new HashMap<>();
            for (String item : inventory.getAllKeys()) {
                String[] parts = item.split(":", -1);
                if (parts.length != 2 || !inventory.contains(item, Tag.TAG_INT)) {
                    throw new IllegalArgumentException("Invalid original shop storage key");
                }
                int kind = Integer.parseInt(parts[0]);
                int id = Integer.parseInt(parts[1]);
                int count = inventory.getInt(item);
                if (kind < 0 || kind > 2 || id < 1 || count < 1 || count > 999) {
                    throw new IllegalArgumentException("Invalid original shop storage count");
                }
                entries.put(new ItemKey(kind, id), count);
            }
            if (!entries.isEmpty()) data.inventories.put(owner, entries);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag owners = new CompoundTag();
        inventories.forEach((owner, inventory) -> {
            CompoundTag entries = new CompoundTag();
            inventory.forEach((key, count) -> entries.putInt(key.kind() + ":" + key.id(), count));
            owners.put(owner.toString(), entries);
        });
        tag.put("Owners", owners);
        return tag;
    }

    public record ItemKey(int kind, int id) {
    }
}
