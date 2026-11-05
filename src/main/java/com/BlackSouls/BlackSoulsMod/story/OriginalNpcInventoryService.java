package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.capability.OriginalNpcProgress;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcItems;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcShopRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public final class OriginalNpcInventoryService {
    private static final BigInteger NORMAL_LIMIT = BigInteger.valueOf(OriginalNpcShopRules.NORMAL_LIMIT);
    private static final List<String> CURIO_ORDER = List.of("head", "body", "ring", "watch");

    private OriginalNpcInventoryService() {
    }

    public static int count(ServerPlayer player, int kind, int id) {
        Item item = OriginalNpcItems.item(kind, id);
        if (player == null || item == null || OriginalNpcShopRules.get(kind, id) == null) {
            throw new UnsupportedOperationException("unmapped_original_inventory:" + kind + ":" + id);
        }
        long count = 0;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (equippedMainHand(player, kind, slot)) continue;
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        if (count > Integer.MAX_VALUE) throw new UnsupportedOperationException("original_inventory_count_overflow");
        return (int) count;
    }

    public static boolean change(ServerPlayer player, int kind, int id, long amount, boolean includeEquipment) {
        Item item = OriginalNpcItems.item(kind, id);
        OriginalNpcShopRules.Rule rule = OriginalNpcShopRules.get(kind, id);
        if (player == null || item == null || rule == null || kind < 0 || kind > 2 || id < 1) return false;
        int current = count(player, kind, id);
        OriginalNpcShopStorageData storage = OriginalNpcShopStorageData.get(player.server);
        int stored = storage.count(player.getUUID(), kind, id);
        int nextStored = stored;
        BigInteger sourceTotal = BigInteger.valueOf(current).add(BigInteger.valueOf(amount));
        if (!rule.storageForbidden() && !OriginalNpcProgress.get(player.server).switchValue(202)
                && sourceTotal.compareTo(NORMAL_LIMIT) > 0) {
            BigInteger overflow = sourceTotal.subtract(NORMAL_LIMIT);
            nextStored = BigInteger.valueOf(stored).add(overflow)
                    .min(BigInteger.valueOf(rule.storageLimit())).intValueExact();
            sourceTotal = NORMAL_LIMIT;
        }
        int nextCount = sourceTotal.max(BigInteger.ZERO).min(NORMAL_LIMIT).intValueExact();
        List<ItemStack> inventory = planInventory(player, kind, item, nextCount - current);
        if (inventory == null) return false;
        List<EquipmentChange> equipment = new ArrayList<>();
        if (includeEquipment && sourceTotal.signum() < 0) {
            planEquipment(player, kind, item, sourceTotal.negate(), inventory, equipment);
        }
        for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().items.set(slot, inventory.get(slot));
        for (EquipmentChange change : equipment) change.setter().accept(change.stack());
        if (nextStored != stored) storage.setCount(player.getUUID(), kind, id, nextStored);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return true;
    }

    private static List<ItemStack> planInventory(ServerPlayer player, int kind, Item item, int delta) {
        List<ItemStack> plan = player.getInventory().items.stream().map(ItemStack::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        long remaining = Math.abs((long) delta);
        ItemStack template = new ItemStack(item);
        for (int pass = 0; pass < (delta > 0 ? 2 : 1) && remaining > 0; pass++) {
            for (int slot = 0; slot < plan.size() && remaining > 0; slot++) {
                if (equippedMainHand(player, kind, slot)) continue;
                ItemStack stack = plan.get(slot);
                if (delta < 0 && stack.is(item)) {
                    int removed = (int) Math.min(remaining, stack.getCount());
                    stack.shrink(removed);
                    remaining -= removed;
                } else if (delta > 0 && (pass == 0 && !stack.isEmpty() && ItemStack.isSameItemSameTags(template, stack)
                        || pass == 1 && stack.isEmpty())) {
                    int room = Math.min(item.getMaxStackSize(), player.getInventory().getMaxStackSize()) - stack.getCount();
                    int added = (int) Math.min(remaining, Math.max(0, room));
                    if (stack.isEmpty()) plan.set(slot, new ItemStack(item, added));
                    else stack.grow(added);
                    remaining -= added;
                }
            }
        }
        return remaining == 0 ? plan : null;
    }

    private static void planEquipment(ServerPlayer player, int kind, Item item, BigInteger remaining,
                                      List<ItemStack> inventory, List<EquipmentChange> changes) {
        int selected = player.getInventory().selected;
        if (kind == 1 && selected >= 0 && selected < inventory.size()) {
            ItemStack selectedStack = inventory.get(selected);
            if (selectedStack.is(item)) {
                int removed = remaining.min(BigInteger.valueOf(selectedStack.getCount())).intValueExact();
                selectedStack.shrink(removed);
                remaining = remaining.subtract(BigInteger.valueOf(removed));
            }
        }
        for (EquipmentSlot slot : List.of(EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            remaining = removeEquipment(item, remaining, player.getItemBySlot(slot), stack -> player.setItemSlot(slot, stack), changes);
        }
        var curios = CuriosApi.getCuriosInventory(player).orElse(null);
        if (curios == null || remaining.signum() == 0) return;
        var handlers = new ArrayList<>(curios.getCurios().entrySet());
        handlers.sort(Comparator.<java.util.Map.Entry<String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>>comparingInt(
                entry -> curioOrder(entry.getKey())).thenComparing(java.util.Map.Entry::getKey));
        for (var handler : handlers) {
            var stacks = handler.getValue().getStacks();
            for (int index = 0; index < stacks.getSlots() && remaining.signum() > 0; index++) {
                int slot = index;
                remaining = removeEquipment(item, remaining, stacks.getStackInSlot(slot),
                        stack -> stacks.setStackInSlot(slot, stack), changes);
            }
        }
    }

    private static BigInteger removeEquipment(Item item, BigInteger remaining, ItemStack current,
                                               Consumer<ItemStack> setter, List<EquipmentChange> changes) {
        if (remaining.signum() == 0 || !current.is(item)) return remaining;
        int removed = remaining.min(BigInteger.valueOf(current.getCount())).intValueExact();
        ItemStack result = current.copy();
        result.shrink(removed);
        changes.add(new EquipmentChange(setter, result));
        return remaining.subtract(BigInteger.valueOf(removed));
    }

    private static int curioOrder(String name) {
        int index = CURIO_ORDER.indexOf(name);
        return index < 0 ? CURIO_ORDER.size() : index;
    }

    private static boolean equippedMainHand(ServerPlayer player, int kind, int slot) {
        return kind == 1 && slot == player.getInventory().selected;
    }

    private record EquipmentChange(Consumer<ItemStack> setter, ItemStack stack) {
    }
}
