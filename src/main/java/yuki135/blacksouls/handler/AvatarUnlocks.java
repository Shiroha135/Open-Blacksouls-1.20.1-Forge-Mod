package yuki135.blacksouls.handler;

import net.minecraft.server.level.ServerPlayer;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.PacketSyncUnlockedAvatars;
import yuki135.blacksouls.party.PartyManager;
import yuki135.blacksouls.util.AvatarCatalog;

public final class AvatarUnlocks {
    private AvatarUnlocks() { }

    public static String selected(ServerPlayer player) {return AvatarCatalog.selected(stats(player));}

    public static String select(ServerPlayer player,String avatar) {
        var stats=stats(player);
        if(stats==null)return "unknown";
        stats.avatarId=AvatarCatalog.unlocked(stats).contains(avatar)?avatar:AvatarCatalog.selected(stats);
        StatEventHandler.syncToClient(player);
        return stats.avatarId;
    }

    public static void sync(ServerPlayer player) {
        var stats=stats(player);if(stats==null)return;
        stats.avatarId=AvatarCatalog.selected(stats);
        StatEventHandler.syncToClient(player);
        NetworkHandler.sendToPlayer(new PacketSyncUnlockedAvatars(AvatarCatalog.unlocked(stats)),player);
        PartyManager.refresh(player);
    }

    public static boolean setAll(ServerPlayer player,boolean unlocked) {
        var stats=stats(player);if(stats==null)return false;
        boolean before=stats.allAvatarsUnlocked;String previous=stats.avatarId;
        stats.allAvatarsUnlocked=unlocked;
        if(!unlocked)stats.avatarId=AvatarCatalog.initial(stats);
        if(!yuki135.blacksouls.util.OriginalOpeningPersistence.saveAndVerify(player)) {
            stats.allAvatarsUnlocked=before;stats.avatarId=previous;
            return false;
        }
        sync(player);
        return true;
    }

    private static BSPlayerStats stats(ServerPlayer player) {return player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);}
}
