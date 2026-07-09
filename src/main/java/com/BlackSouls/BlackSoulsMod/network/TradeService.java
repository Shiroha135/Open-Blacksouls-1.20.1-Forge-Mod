package com.BlackSouls.BlackSoulsMod.network;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncStats;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class TradeService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TRADE_QUANTITY = 999;
    private static Item getItemSafe(String rlString) {
        ResourceLocation rl = ResourceLocation.tryParse(rlString);
        if (rl == null) {
            return null;
        }
        return ForgeRegistries.ITEMS.getValue(rl);
    }

    public static void handleBuy(ServerPlayer player, String itemRLString, int quantity) {
        if (quantity <= 0 || quantity > MAX_TRADE_QUANTITY) {
            LOGGER.warn("[Anti-Cheat] 玩家{}尝试发送非法购买数量:{}", player.getName().getString(), quantity);
            return;
        }

        Item item = getItemSafe(itemRLString);
        if (item == null) return;

        Long unitPrice = BSItemBuyRegistry.BUY_PRICES.get(item);
        if (unitPrice == null) {
            LOGGER.warn("[Anti-Cheat] 玩家{}尝试购买未注册商品:{}", player.getName().getString(), itemRLString);
            return;
        }

        if (unitPrice > Long.MAX_VALUE / quantity) return;
        long totalCost = unitPrice * quantity;

        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            if (stats.souls >= totalCost) {
                stats.souls -= totalCost;

                ItemStack stackToGive = new ItemStack(item, quantity);
                if (!player.getInventory().add(stackToGive)) {
                    player.drop(stackToGive, false);
                }

                NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
            }
        });
    }

    public static void handleSell(ServerPlayer player, String itemRLString, int quantity) {
        if (quantity <= 0 || quantity > MAX_TRADE_QUANTITY) {
            LOGGER.warn("[Anti-Cheat] 玩家{}尝试发送非法出售数量:{}", player.getName().getString(), quantity);
            return;
        }

        Item item = getItemSafe(itemRLString);
        if (item == null) return;

        BSItemSellRegistry.SellInfo info = BSItemSellRegistry.SELL_PRICES.get(item);
        if (info == null) {
            LOGGER.warn("[Anti-Cheat] 玩家{}尝试出售未注册商品:{}", player.getName().getString(), itemRLString);
            return;
        }

        if (info.price > Long.MAX_VALUE / quantity) return;

        int remainingToTake = quantity;

        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) {
                int take = Math.min(stack.getCount(), remainingToTake);
                stack.shrink(take);
                remainingToTake -= take;

                if (remainingToTake <= 0) break;
            }
        }

        if (remainingToTake <= 0) {
            long totalEarn = info.price * quantity;
            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                stats.souls += totalEarn;
                player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
            });
        } else {
            int actuallyRemoved = quantity - remainingToTake;
            if (actuallyRemoved > 0) {
                ItemStack returnStack = new ItemStack(item, actuallyRemoved);
                if (!player.getInventory().add(returnStack)) player.drop(returnStack, false);
            }
            LOGGER.warn("[Anti-Cheat] 玩家{}尝试利用并发漏洞出售不存在的物品！", player.getName().getString());
        }
    }
}
