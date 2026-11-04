package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class OriginalActorBridge {
    private OriginalActorBridge() {
    }

    public static boolean isPlayerActor(ServerPlayer player, int actorId) {
        return player != null && actorId == 2;
    }

    public static long gameData(ServerPlayer player, int actorId, int property) {
        if (!isPlayerActor(player, actorId)) {
            throw new UnsupportedOperationException("unmapped_actor:" + actorId);
        }
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("Missing original actor stats"));
        return switch (property) {
            case 0 -> stats.level;
            case 2 -> (long) player.getHealth();
            case 3 -> (long) stats.mp;
            default -> throw new UnsupportedOperationException("actor_game_data:" + property);
        };
    }

    public static Double hpPercent(ServerPlayer player, int actorId) {
        return isPlayerActor(player, actorId) ? player.getHealth() * 100.0D / player.getMaxHealth() : null;
    }

    public static boolean equipped(ServerPlayer player, int actorId, int kind, int originalItemId) {
        if (!isPlayerActor(player, actorId)) {
            throw new UnsupportedOperationException("unmapped_actor:" + actorId);
        }
        Item item = OriginalNpcItems.item(kind, originalItemId);
        if (item == null || kind < 1 || kind > 2) {
            throw new UnsupportedOperationException("unmapped_actor_equipment:" + kind + ":" + originalItemId);
        }
        if (kind == 1) {
            return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
        }
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.is(item)) {
                return true;
            }
        }
        return CuriosApi.getCuriosInventory(player).map(handler -> !handler.findCurios(item).isEmpty()).orElse(false);
    }
}
