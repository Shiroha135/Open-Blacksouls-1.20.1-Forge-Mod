package com.BlackSouls.BlackSoulsMod.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OriginalNpcShopStorageData extends SavedData {
    private final Map<UUID, Map<ItemKey, Integer>> inventories = new HashMap<>();
    private final Map<UUID, Map<ItemKey, List<ItemStack>>> stackContents = new HashMap<>();

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
        Map<ItemKey, List<ItemStack>> stacks = stackContents.get(owner);
        if (stacks != null && stacks.containsKey(key)) {
            List<ItemStack> kept = new ArrayList<>();
            int remaining = count;
            for (ItemStack stack : stacks.get(key)) {
                if (remaining == 0) break;
                ItemStack copy = stack.copy();
                copy.setCount(Math.min(remaining, copy.getCount()));
                kept.add(copy);
                remaining -= copy.getCount();
            }
            if (kept.isEmpty()) stacks.remove(key);
            else stacks.put(key, List.copyOf(kept));
            if (stacks.isEmpty()) stackContents.remove(owner);
        }
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

    public List<ItemStack> storedStacks(UUID owner, int kind, int id) {
        int count = count(owner, kind, id);
        Item item = OriginalNpcItems.item(kind, id);
        if (item == null) throw new IllegalArgumentException("Unmapped original storage item");
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stackContents.getOrDefault(owner, Map.of()).getOrDefault(new ItemKey(kind, id), List.of())) {
            result.add(stack.copy());
            count -= stack.getCount();
        }
        if (count < 0) throw new IllegalStateException("Original storage stack count exceeds ledger");
        while (count > 0) {
            int amount = Math.min(count, item.getMaxStackSize());
            result.add(OriginalNpcItems.createStack(kind, id, amount));
            count -= amount;
        }
        return List.copyOf(result);
    }

    public void setStacks(UUID owner, int kind, int id, List<ItemStack> stacks) {
        Item item = OriginalNpcItems.item(kind, id);
        if (owner == null || item == null) throw new IllegalArgumentException("Unmapped original storage item");
        int count = 0;
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!OriginalNpcItems.matches(kind, id, stack)) throw new IllegalArgumentException("Mismatched original storage stack");
            count = Math.addExact(count, stack.getCount());
            int remaining = stack.getCount();
            while (remaining > 0) {
                int amount = Math.min(remaining, Math.max(1, Math.min(127, stack.getMaxStackSize())));
                copies.add(stack.copyWithCount(amount));
                remaining -= amount;
            }
        }
        if (count > 999) throw new IllegalArgumentException("Original storage stack count exceeds limit");
        setCount(owner, kind, id, count);
        ItemKey key = new ItemKey(kind, id);
        if (count > 0) stackContents.computeIfAbsent(owner, ignored -> new HashMap<>()).put(key, List.copyOf(copies));
        else {
            Map<ItemKey, List<ItemStack>> current = stackContents.get(owner);
            if (current != null) {
                current.remove(key);
                if (current.isEmpty()) stackContents.remove(owner);
            }
        }
        setDirty();
    }

    public static OriginalNpcShopStorageData load(CompoundTag tag) {
        OriginalNpcShopStorageData data = new OriginalNpcShopStorageData();
        boolean legacy = !tag.contains("WeaponFormsVersion");
        if (!legacy && (!tag.contains("WeaponFormsVersion", Tag.TAG_INT) || tag.getInt("WeaponFormsVersion") != 1)) {
            throw new IllegalArgumentException("Invalid original weapon storage version");
        }
        if (tag.contains("Owners") && !tag.contains("Owners", Tag.TAG_COMPOUND)
                || tag.contains("StackContents") && !tag.contains("StackContents", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Invalid original storage container");
        }
        CompoundTag owners = tag.getCompound("Owners");
        for (String name : owners.getAllKeys()) {
            if (!owners.contains(name, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid original storage owner");
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
        CompoundTag savedStacks = tag.getCompound("StackContents");
        for (String name : savedStacks.getAllKeys()) {
            if (!savedStacks.contains(name, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid original stored stack owner");
            UUID owner = UUID.fromString(name);
            CompoundTag inventory = savedStacks.getCompound(name);
            for (String key : inventory.getAllKeys()) {
                String[] parts = key.split(":", -1);
                if (parts.length != 2 || !inventory.contains(key, Tag.TAG_LIST)) {
                    throw new IllegalArgumentException("Invalid original stored stack key");
                }
                int kind = Integer.parseInt(parts[0]);
                int id = Integer.parseInt(parts[1]);
                Item item = OriginalNpcItems.item(kind, id);
                ListTag list = inventory.getList(key, Tag.TAG_COMPOUND);
                if (item == null || list.isEmpty() || list.size() > 999) {
                    throw new IllegalArgumentException("Invalid original stored stack list");
                }
                List<ItemStack> stacks = new ArrayList<>();
                int count = 0;
                for (int index = 0; index < list.size(); index++) {
                    ItemStack stack = ItemStack.of(list.getCompound(index));
                    if (stack.isEmpty() || !stack.is(item)) throw new IllegalArgumentException("Invalid original stored stack");
                    count = Math.addExact(count, stack.getCount());
                    stacks.add(stack);
                }
                if (count > data.count(owner, kind, id)) throw new IllegalArgumentException("Original storage payload exceeds ledger");
                data.stackContents.computeIfAbsent(owner, ignored -> new HashMap<>()).put(new ItemKey(kind, id), List.copyOf(stacks));
            }
        }
        for (var owner : new ArrayList<>(data.stackContents.entrySet())) {
            Map<ItemKey, List<ItemStack>> classified = new HashMap<>();
            Map<ItemKey, Integer> counts = new HashMap<>(data.inventories.getOrDefault(owner.getKey(), Map.of()));
            for (var payload : owner.getValue().entrySet()) {
                ItemKey original = payload.getKey();
                for (ItemStack stack : payload.getValue()) {
                    ItemKey target = original;
                    if (!OriginalNpcItems.matches(original.kind(), original.id(), stack)) {
                        int source = original.kind() == 1 ? OriginalNpcItems.sourceId(stack) : -1;
                        if (!legacy || source < 1) throw new IllegalArgumentException("Invalid original stored weapon form");
                        target = new ItemKey(1, source);
                        int old = counts.getOrDefault(original, 0) - stack.getCount();
                        if (old == 0) counts.remove(original); else counts.put(original, old);
                        int migrated = Math.addExact(counts.getOrDefault(target, 0), stack.getCount());
                        counts.put(target, migrated);
                        data.setDirty();
                    }
                    classified.computeIfAbsent(target, ignored -> new ArrayList<>()).add(stack.copy());
                }
            }
            if (counts.values().stream().anyMatch(count -> count < 1 || count > 999)) {
                throw new IllegalArgumentException("Original weapon migration exceeds storage limit");
            }
            if (counts.isEmpty()) data.inventories.remove(owner.getKey());
            else data.inventories.put(owner.getKey(), counts);
            data.stackContents.put(owner.getKey(), classified);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("WeaponFormsVersion", 1);
        CompoundTag owners = new CompoundTag();
        inventories.forEach((owner, inventory) -> {
            CompoundTag entries = new CompoundTag();
            inventory.forEach((key, count) -> entries.putInt(key.kind() + ":" + key.id(), count));
            owners.put(owner.toString(), entries);
        });
        tag.put("Owners", owners);
        CompoundTag payload = new CompoundTag();
        stackContents.forEach((owner, inventory) -> {
            CompoundTag entries = new CompoundTag();
            inventory.forEach((key, stacks) -> {
                ListTag list = new ListTag();
                for (ItemStack stack : stacks) list.add(stack.save(new CompoundTag()));
                entries.put(key.kind() + ":" + key.id(), list);
            });
            payload.put(owner.toString(), entries);
        });
        tag.put("StackContents", payload);
        return tag;
    }

    public record ItemKey(int kind, int id) {
    }
}
