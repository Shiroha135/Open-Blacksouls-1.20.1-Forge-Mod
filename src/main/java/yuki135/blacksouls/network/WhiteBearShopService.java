package yuki135.blacksouls.network;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.compat.scene.OriginalWhiteBearPlacement;
import yuki135.blacksouls.network.packets.PacketOpenWhiteBearDialogue;
import yuki135.blacksouls.network.packets.PacketSyncStats;
import net.minecraft.server.level.ServerPlayer;

public final class WhiteBearShopService {
    public static void interact(ServerPlayer player) {
        if (OriginalWhiteBearPlacement.isOriginalSiteContext(player)) {
            OriginalWhiteBearPlacement.interact(player);
            return;
        }
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            stats.whiteBearProgress = Math.max(
                    stats.whiteBearProgress,
                    Math.min(12, player.getInventory().countItem(BlackSouls.ALICE_ITEM.get()))
            );
            boolean firstVisit = !stats.whiteBearIntroduced;
            boolean freeSouls = false;
            if (firstVisit) {
                stats.whiteBearIntroduced = true;
            } else if (stats.souls <= 0 && !stats.whiteBearFreeSoulsClaimed) {
                stats.souls += 500;
                stats.whiteBearFreeSoulsClaimed = true;
                freeSouls = true;
            }
            NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
            NetworkHandler.sendToPlayer(
                    new PacketOpenWhiteBearDialogue(firstVisit, freeSouls, stats.whiteBearProgress),
                    player
            );
        });
    }

    public static void setProgress(ServerPlayer player, int progress) {
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            stats.whiteBearProgress = Math.max(0, Math.min(12, progress));
            NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
        });
    }

    public static void resetDialogue(ServerPlayer player) {
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            stats.whiteBearIntroduced = false;
            stats.whiteBearFreeSoulsClaimed = false;
            NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
        });
    }

    private WhiteBearShopService() {
    }
}
