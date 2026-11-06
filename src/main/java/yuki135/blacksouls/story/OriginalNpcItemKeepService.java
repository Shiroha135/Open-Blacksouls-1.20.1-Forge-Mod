package yuki135.blacksouls.story;

import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPacket;
import yuki135.blacksouls.network.packets.ServerboundOriginalNpcTradePacket;
import yuki135.blacksouls.util.OriginalNpcItems;
import yuki135.blacksouls.util.OriginalNpcShopRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class OriginalNpcItemKeepService {
    private OriginalNpcItemKeepService() {
    }

    public static ClientboundOriginalNpcPacket.KeepView view(ServerPlayer player) {
        OriginalNpcShopStorageData storage = OriginalNpcShopStorageData.get(player.server);
        List<ClientboundOriginalNpcPacket.KeepOffer> offers = new ArrayList<>();
        for (OriginalNpcItems.Entry entry : OriginalNpcItems.values()) {
            Item item = OriginalNpcItems.item(entry.kind(), entry.id());
            OriginalNpcShopRules.Rule rule = OriginalNpcShopRules.get(entry.kind(), entry.id());
            if (item == null || rule == null) continue;
            int carried = OriginalNpcInventoryService.count(player, entry.kind(), entry.id());
            int stored = storage.count(player.getUUID(), entry.kind(), entry.id());
            if (carried == 0 && stored == 0) continue;
            int deposit = rule.storageForbidden() ? 0 : Math.max(0, Math.min(carried, rule.storageLimit() - stored));
            int withdraw = rule.storageForbidden() ? 0 : Math.max(0, Math.min(stored, OriginalNpcShopRules.NORMAL_LIMIT - carried));
            int lower = 0;
            int upper = withdraw;
            while (lower < upper) {
                int amount = (lower + upper + 1) / 2;
                if (plan(player, entry.kind(), entry.id(), amount, false) != null) lower = amount;
                else upper = amount - 1;
            }
            offers.add(new ClientboundOriginalNpcPacket.KeepOffer(entry.kind(), entry.id(),
                    rule.category(entry.kind()), entry.name(), entry.registry().toString(), carried, stored,
                    deposit, lower, rule.storageLimit(), rule.storageForbidden()));
        }
        return new ClientboundOriginalNpcPacket.KeepView(offers);
    }

    public static boolean transfer(ServerPlayer player, ServerboundOriginalNpcTradePacket.Action action,
                                   int kind, int id, int quantity) {
        if (player == null || quantity < 1 || quantity > 999 || kind < 0 || kind > 2 || id < 1
                || action != ServerboundOriginalNpcTradePacket.Action.DEPOSIT
                && action != ServerboundOriginalNpcTradePacket.Action.WITHDRAW) return false;
        OriginalNpcShopRules.Rule rule = OriginalNpcShopRules.get(kind, id);
        if (rule == null || rule.storageForbidden() || OriginalNpcItems.item(kind, id) == null) return false;
        int carried = OriginalNpcInventoryService.count(player, kind, id);
        int stored = OriginalNpcShopStorageData.get(player.server).count(player.getUUID(), kind, id);
        boolean deposit = action == ServerboundOriginalNpcTradePacket.Action.DEPOSIT;
        if (deposit ? quantity > carried || (long) stored + quantity > rule.storageLimit()
                : quantity > stored || (long) carried + quantity > OriginalNpcShopRules.NORMAL_LIMIT) return false;
        TransferPlan planned = plan(player, kind, id, quantity, deposit);
        if (planned == null) return false;
        OriginalNpcShopStorageData.get(player.server).setStacks(player.getUUID(), kind, id, planned.stored());
        for (int slot = 0; slot < planned.inventory().size(); slot++) {
            player.getInventory().items.set(slot, planned.inventory().get(slot));
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return true;
    }

    private static TransferPlan plan(ServerPlayer player, int kind, int id, int quantity, boolean deposit) {
        Item item = OriginalNpcItems.item(kind, id);
        List<ItemStack> inventory = player.getInventory().items.stream().map(ItemStack::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<ItemStack> stored = new ArrayList<>(OriginalNpcShopStorageData.get(player.server)
                .storedStacks(player.getUUID(), kind, id));
        int remaining = quantity;
        if (deposit) {
            for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
                if (equippedMainHand(player, kind, slot)) continue;
                ItemStack stack = inventory.get(slot);
                if (!OriginalNpcItems.matches(kind, id, stack)) continue;
                int amount = Math.min(remaining, stack.getCount());
                stored.add(stack.copyWithCount(amount));
                stack.shrink(amount);
                remaining -= amount;
            }
        } else {
            for (ItemStack stack : stored) {
                int amount = Math.min(remaining, stack.getCount());
                if (amount == 0) continue;
                ItemStack moving = stack.copyWithCount(amount);
                if (!insert(player, kind, inventory, moving)) return null;
                stack.shrink(amount);
                remaining -= amount;
            }
            stored.removeIf(ItemStack::isEmpty);
        }
        return remaining == 0 ? new TransferPlan(inventory, stored) : null;
    }

    private static boolean insert(ServerPlayer player, int kind, List<ItemStack> inventory, ItemStack moving) {
        int remaining = moving.getCount();
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
                if (equippedMainHand(player, kind, slot)) continue;
                ItemStack stack = inventory.get(slot);
                if (pass == 0 ? stack.isEmpty() || !ItemStack.isSameItemSameTags(moving, stack) : !stack.isEmpty()) continue;
                int capacity = Math.min(moving.getMaxStackSize(), player.getInventory().getMaxStackSize());
                int amount = Math.min(remaining, Math.max(0, capacity - stack.getCount()));
                if (stack.isEmpty()) inventory.set(slot, moving.copyWithCount(amount));
                else stack.grow(amount);
                remaining -= amount;
            }
        }
        return remaining == 0;
    }

    private static boolean equippedMainHand(ServerPlayer player, int kind, int slot) {
        return kind == 1 && player.getInventory().selected == slot;
    }

    private record TransferPlan(List<ItemStack> inventory, List<ItemStack> stored) {
    }
}
