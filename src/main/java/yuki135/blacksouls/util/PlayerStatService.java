package yuki135.blacksouls.util;

import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundBannerPacket;
import yuki135.blacksouls.network.packets.PacketSyncStats;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerStatService {
    public static void addSen(ServerPlayer player, int delta) {
        if (delta == 0) {
            return;
        }
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            stats.sen = Math.max(0, stats.sen + delta);
            StatEventHandler.applyStats(player);
            NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
            NetworkHandler.sendToPlayer(
                    new ClientboundBannerPacket(ClientboundBannerPacket.Type.SEEK_SERVICE, delta),
                    player
            );
        });
    }

    public static void recoverAll(ServerPlayer player) {
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            StatEventHandler.applyStats(player);
            stats.mp = stats.maxMp;
            stats.currentActionPoints = stats.getMaxActionPoints();
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
            NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
        });
    }

    private PlayerStatService() {
    }
}
