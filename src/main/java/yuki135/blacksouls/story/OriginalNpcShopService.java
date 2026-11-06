package yuki135.blacksouls.story;

import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPacket;
import yuki135.blacksouls.network.packets.ServerboundOriginalNpcTradePacket;
import yuki135.blacksouls.util.OriginalNpcItems;
import yuki135.blacksouls.util.OriginalNpcExchangeData;
import yuki135.blacksouls.util.OriginalNpcShopRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OriginalNpcShopService {
    private OriginalNpcShopService() {
    }

    public static ClientboundOriginalNpcPacket.ShopView view(ServerPlayer player, List<OriginalNpcInterpreter.Goods> goods,
                                                             boolean storage, boolean purchaseOnly) {
        List<ClientboundOriginalNpcPacket.TradeOffer> buys = new ArrayList<>();
        List<ClientboundOriginalNpcPacket.TradeOffer> sells = new ArrayList<>();
        for (OriginalNpcInterpreter.Goods offer : goods) {
            OriginalNpcItems.Entry item = OriginalNpcItems.get(offer.kind(), offer.id());
            if (item == null || OriginalNpcItems.item(offer.kind(), offer.id()) == null) {
                throw new UnsupportedOperationException("unmapped_shop_goods");
            }
            long price = buyingPrice(goods, offer.kind(), offer.id());
            int owned = count(player, storage, offer.kind(), offer.id());
            int maximum = maxBuy(player, storage, item, price);
            List<ClientboundOriginalNpcPacket.SynthesisMaterial> materials = new ArrayList<>();
            for (var material : OriginalNpcExchangeData.materials(item.kind(), item.id())) {
                var input = OriginalNpcItems.get(material.kind(), material.id());
                if (OriginalNpcItems.item(material.kind(), material.id()) == null) {
                    throw new UnsupportedOperationException("unmapped_exchange_material");
                }
                materials.add(new ClientboundOriginalNpcPacket.SynthesisMaterial(material.kind(), material.id(),
                        input.name(), input.registry().toString(), material.quantity(),
                        count(player, storage, material.kind(), material.id())));
            }
            buys.add(new ClientboundOriginalNpcPacket.TradeOffer(item.kind(), item.id(),
                    OriginalNpcShopRules.category(item.kind(), item.id()), item.name(), item.registry().toString(),
                    price, owned, maximum, maximum > 0, materials, OriginalNpcEquipmentService.view(player, item.kind(), item.id())));
        }
        if (!purchaseOnly) {
            for (OriginalNpcItems.Entry item : OriginalNpcItems.values()) {
                if (OriginalNpcItems.item(item.kind(), item.id()) == null) continue;
                int owned = count(player, storage, item.kind(), item.id());
                if (owned > 0) sells.add(offer(player, item, item.price() / 2L, owned,
                        Math.min(999, owned), item.price() > 0L));
            }
        }
        return new ClientboundOriginalNpcPacket.ShopView(storage, purchaseOnly,
                !OriginalNpcProgress.get(player.server).switchValue(201), exchange(player), Math.max(0L, stats(player).souls), buys, sells);
    }

    private static ClientboundOriginalNpcPacket.TradeOffer offer(ServerPlayer player, OriginalNpcItems.Entry item, long price,
                                                                 int owned, int maximum, boolean enabled) {
        return new ClientboundOriginalNpcPacket.TradeOffer(item.kind(), item.id(),
                OriginalNpcShopRules.category(item.kind(), item.id()), item.name(), item.registry().toString(),
                price, owned, maximum, enabled, List.of(), OriginalNpcEquipmentService.view(player, item.kind(), item.id()));
    }

    public static boolean transact(ServerPlayer player, List<OriginalNpcInterpreter.Goods> goods,
                                   boolean storage, boolean purchaseOnly, ServerboundOriginalNpcTradePacket.Action action,
                                   int kind, int id, int quantity) {
        if (quantity < 1 || quantity > 999 || kind < 0 || kind > 2 || id < 1) return false;
        OriginalNpcItems.Entry entry = OriginalNpcItems.get(kind, id);
        Item item = OriginalNpcItems.item(kind, id);
        if (entry == null || item == null) return false;
        BSPlayerStats stats = stats(player);
        long nextGold;
        int current = count(player, storage, kind, id);
        int nextCount;
        List<ItemStack> planned = null;
        Map<OriginalNpcShopStorageData.ItemKey, Integer> storedPlan = new LinkedHashMap<>();
        if (action == ServerboundOriginalNpcTradePacket.Action.BUY) {
            long price = buyingPrice(goods, kind, id);
            if (price < 0L || quantity > maxBuy(player, storage, entry, price)) return false;
            long total = Math.multiplyExact(price, quantity);
            if (stats.souls < total) return false;
            nextGold = clampGold(stats.souls - total);
            nextCount = Math.addExact(current, quantity);
            if (storage) {
                if (exchange(player)) {
                    for (var material : OriginalNpcExchangeData.materials(kind, id)) {
                        var key = new OriginalNpcShopStorageData.ItemKey(material.kind(), material.id());
                        storedPlan.put(key, count(player, true, material.kind(), material.id())
                                - Math.multiplyExact(material.quantity(), quantity));
                    }
                }
                var output = new OriginalNpcShopStorageData.ItemKey(kind, id);
                storedPlan.put(output, Math.addExact(storedPlan.getOrDefault(output, current), quantity));
            } else planned = planPurchase(player, kind, id, item, quantity);
        } else if (action == ServerboundOriginalNpcTradePacket.Action.SELL) {
            if (purchaseOnly || entry.price() <= 0L || quantity > current) return false;
            long income = Math.multiplyExact(entry.price() / 2L, quantity);
            nextGold = stats.souls >= OriginalNpcShopRules.MAX_GOLD - income
                    ? OriginalNpcShopRules.MAX_GOLD : clampGold(stats.souls + income);
            nextCount = current - quantity;
            if (!storage) planned = planInventory(player, kind, id, item, -quantity);
        } else return false;
        if (!storage && planned == null) return false;
        if (storage) {
            if (storedPlan.isEmpty()) storedPlan.put(new OriginalNpcShopStorageData.ItemKey(kind, id), nextCount);
            if (storedPlan.values().stream().anyMatch(value -> value < 0 || value > 999)) return false;
            var container = OriginalNpcShopStorageData.get(player.server);
            storedPlan.forEach((key, value) -> container.setCount(player.getUUID(), key.kind(), key.id(), value));
        } else {
            for (int slot = 0; slot < planned.size(); slot++) player.getInventory().items.set(slot, planned.get(slot));
            player.getInventory().setChanged();
        }
        stats.souls = nextGold;
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return true;
    }

    public static int count(ServerPlayer player, boolean storage, int kind, int id) {
        if (storage) return OriginalNpcShopStorageData.get(player.server).count(player.getUUID(), kind, id);
        Item item = OriginalNpcItems.item(kind, id);
        if (item == null) return 0;
        int count = 0;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (equippedMainHand(player, kind, slot)) continue;
            ItemStack stack = player.getInventory().items.get(slot);
            if (OriginalNpcItems.matches(kind, id, stack)) count = Math.addExact(count, stack.getCount());
        }
        return count;
    }

    private static int maxBuy(ServerPlayer player, boolean storage, OriginalNpcItems.Entry entry, long price) {
        if (price < 0L) return 0;
        OriginalNpcShopRules.Rule rule = OriginalNpcShopRules.get(entry.kind(), entry.id());
        if (rule == null || storage && rule.storageForbidden()) return 0;
        int maximum = Math.max(0, (storage ? rule.storageLimit() : OriginalNpcShopRules.NORMAL_LIMIT)
                - count(player, storage, entry.kind(), entry.id()));
        if (price > 0L) maximum = (int) Math.min(maximum, Math.max(0L, stats(player).souls) / price);
        if (exchange(player)) {
            for (var material : OriginalNpcExchangeData.materials(entry.kind(), entry.id())) {
                if (OriginalNpcItems.item(material.kind(), material.id()) == null) return 0;
                maximum = Math.min(maximum, count(player, storage, material.kind(), material.id()) / material.quantity());
            }
            if (!storage) {
                Item item = OriginalNpcItems.item(entry.kind(), entry.id());
                while (maximum > 0 && planPurchase(player, entry.kind(), entry.id(), item, maximum) == null) maximum--;
            }
            return maximum;
        }
        if (!storage) {
            Item item = OriginalNpcItems.item(entry.kind(), entry.id());
            int capacity = 0;
            ItemStack template = OriginalNpcItems.createStack(entry.kind(), entry.id(), 1);
            for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
                if (equippedMainHand(player, entry.kind(), slot)) continue;
                ItemStack stack = player.getInventory().items.get(slot);
                if (stack.isEmpty()) capacity += Math.min(item.getMaxStackSize(), player.getInventory().getMaxStackSize());
                else if (ItemStack.isSameItemSameTags(template, stack)) capacity += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
            maximum = Math.min(maximum, capacity);
        }
        return maximum;
    }

    private static List<ItemStack> planInventory(ServerPlayer player, int kind, int id, Item item, int delta) {
        List<ItemStack> plan = player.getInventory().items.stream().map(ItemStack::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return applyInventory(player, plan, kind, id, item, delta) ? plan : null;
    }

    private static List<ItemStack> planPurchase(ServerPlayer player, int kind, int id, Item item, int quantity) {
        List<ItemStack> plan = player.getInventory().items.stream().map(ItemStack::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (exchange(player)) {
            for (var material : OriginalNpcExchangeData.materials(kind, id)) {
                if (!applyInventory(player, plan, material.kind(), material.id(),
                        OriginalNpcItems.item(material.kind(), material.id()), -Math.multiplyExact(material.quantity(), quantity))) return null;
            }
        }
        return applyInventory(player, plan, kind, id, item, quantity) ? plan : null;
    }

    private static boolean applyInventory(ServerPlayer player, List<ItemStack> plan, int kind, int id, Item item, int delta) {
        if (item == null) return false;
        int remaining = Math.abs(delta);
        ItemStack template = OriginalNpcItems.createStack(kind, id, 1);
        for (int pass = 0; pass < (delta > 0 ? 2 : 1) && remaining > 0; pass++) {
            for (int slot = 0; slot < plan.size() && remaining > 0; slot++) {
                if (equippedMainHand(player, kind, slot)) continue;
                ItemStack stack = plan.get(slot);
                if (delta < 0 && OriginalNpcItems.matches(kind, id, stack)) {
                    int removed = Math.min(remaining, stack.getCount());
                    stack.shrink(removed);
                    remaining -= removed;
                } else if (delta > 0 && (pass == 0 && !stack.isEmpty() && ItemStack.isSameItemSameTags(template, stack)
                        || pass == 1 && stack.isEmpty())) {
                    int room = Math.min(item.getMaxStackSize(), player.getInventory().getMaxStackSize()) - stack.getCount();
                    int added = Math.min(remaining, Math.max(0, room));
                    if (stack.isEmpty()) plan.set(slot, template.copyWithCount(added));
                    else stack.grow(added);
                    remaining -= added;
                }
            }
        }
        return remaining == 0;
    }

    private static boolean exchange(ServerPlayer player) {
        return OriginalNpcProgress.get(player.server).switchValue(935);
    }

    private static boolean equippedMainHand(ServerPlayer player, int kind, int slot) {
        return kind == 1 && slot == player.getInventory().selected;
    }

    private static long buyingPrice(List<OriginalNpcInterpreter.Goods> goods, int kind, int id) {
        long price = -1L;
        OriginalNpcItems.Entry item = OriginalNpcItems.get(kind, id);
        if (item == null) return -1L;
        for (OriginalNpcInterpreter.Goods offer : goods) {
            if (offer.kind() == kind && offer.id() == id) price = offer.price() >= 0L ? offer.price() : item.price();
        }
        return price;
    }

    private static long clampGold(long value) {
        return Math.max(0L, Math.min(OriginalNpcShopRules.MAX_GOLD, value));
    }

    private static BSPlayerStats stats(ServerPlayer player) {
        return player.getCapability(BSPlayerStats.CAPABILITY).orElseThrow(() -> new IllegalStateException("Missing original shop player stats"));
    }
}
