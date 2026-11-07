package yuki135.blacksouls.util;

import yuki135.blacksouls.capability.BSPlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import top.theillusivec4.curios.api.CuriosApi;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.PacketSyncStats;

public final class OriginalActorBridge {
    private OriginalActorBridge() {
    }

    public static boolean isPlayerActor(ServerPlayer player, int actorId) {
        return player != null && actorId == OriginalOpeningCareers.actorId(player);
    }

    public static String changeExperience(ServerPlayer player, int actorId, long amount, boolean show) {
        return applyExperience(player, actorId, amount, show, 0L);
    }

    public static String convertExperience(ServerPlayer player, int actorId, long amount, boolean show) {
        if (amount < 0) throw new UnsupportedOperationException("negative_soul_experience");
        return applyExperience(player, actorId, amount, show, amount);
    }

    private static String applyExperience(ServerPlayer player, int actorId, long amount, boolean show, long soulCost) {
        if (actorId == 0) actorId = OriginalOpeningCareers.actorId(player);
        if (!isPlayerActor(player, actorId)) throw new UnsupportedOperationException("unmapped_actor_experience:" + actorId);
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElseThrow(() -> new IllegalStateException("Missing original actor stats"));
        if (stats.currentExp < 0) throw new UnsupportedOperationException("invalid_actor_experience");
        if (soulCost > 0 && stats.souls < soulCost) throw new UnsupportedOperationException("experience_funding_changed");
        long next = amount < -stats.currentExp ? 0L : Math.addExact(stats.currentExp, amount);
        int before = stats.level;
        float health = player.getHealth();
        double mana = stats.mp;
        stats.changeExp(next);
        stats.souls -= soulCost;
        StatEventHandler.applyStats(player);
        stats.mp = Math.max(0.0D, Math.min(mana, stats.maxMp));
        player.setHealth(Math.min(health, player.getMaxHealth()));
        NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
        return show && stats.level > before ? StoryNameData.get(player) + "提高到了等级 " + stats.level + " ！" : "";
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
            return OriginalNpcItems.matches(kind, originalItemId, player.getMainHandItem()) || OriginalNpcItems.matches(kind, originalItemId, player.getOffhandItem());
        }
        if (item instanceof ShieldItem && OriginalNpcItems.matches(kind, originalItemId, player.getOffhandItem())) return true;
        for (ItemStack stack : player.getArmorSlots()) {
            if (OriginalNpcItems.matches(kind, originalItemId, stack)) {
                return true;
            }
        }
        return CuriosApi.getCuriosInventory(player).map(handler -> !handler.findCurios(stack -> OriginalNpcItems.matches(kind, originalItemId, stack)).isEmpty()).orElse(false);
    }
}
