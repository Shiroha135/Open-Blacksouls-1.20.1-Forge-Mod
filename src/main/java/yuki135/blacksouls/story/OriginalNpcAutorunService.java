package yuki135.blacksouls.story;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.entity.EntityOriginalNpc;

public final class OriginalNpcAutorunService {
    public static final String CHICKEN="map031_ev093";

    private OriginalNpcAutorunService() {
    }

    public static boolean isManaged(String sourceId) { return CHICKEN.equals(sourceId); }

    static boolean unique(EntityOriginalNpc npc) {
        if (!(npc.level() instanceof ServerLevel level)) return false;
        EntityOriginalNpc selected=null;
        for (var entity:level.getAllEntities()) if (entity instanceof EntityOriginalNpc candidate
                && isManaged(candidate.getNpcId()) && candidate.isAlive() && !candidate.isRemoved() && !candidate.isStaticMapProxy()) {
            if (selected!=null) return false;
            selected=candidate;
        }
        return selected==npc;
    }

    static boolean movementReserved(ServerPlayer player,EntityOriginalNpc npc,boolean restoring) {
        var data=OriginalNpcMovementData.get(player.server);
        for (var id:data.activeTickets) {
            var ticket=data.tickets.get(id);
            if (ticket==null || ticket.complete || !ticket.replacement.isEmpty()
                    || !ticket.target.equals(player.getUUID()) && !ticket.target.equals(npc.getUUID())) continue;
            if (!restoring || !ticket.owner.equals(player.getUUID()) || ticket.sourceMap!=31
                    || !ticket.dimension.equals(player.level().dimension().location().toString())) return true;
        }
        return false;
    }

    static boolean eligible(ServerPlayer player,EntityOriginalNpc npc) {
        return context(player,npc) && !OriginalNpcService.isConversing(player);
    }

    static boolean context(ServerPlayer player,EntityOriginalNpc npc) {
        return player!=null && npc!=null && player.server.isSameThread() && player.server.getPlayerList().getPlayer(player.getUUID())==player
                && player.isAlive() && !player.isRemoved() && !player.isSpectator() && player.level()==npc.level()
                && isManaged(npc.getNpcId()) && npc.isAlive() && !npc.isRemoved() && !npc.isStaticMapProxy()
                && player.serverLevel().getEntity(npc.getUUID())==npc && player.serverLevel().areEntitiesLoaded(npc.chunkPosition().toLong())
                && (player.level().dimension().location()+"#map_031").equals(player.getPersistentData().getString("Blacksouls2CurrentScene"))
                && !OriginalOpeningService.managed(player) && !TurnBattleManager.isInBattle(player)
                && !OriginalWorldTransferService.pending(player);
    }

    public static void tick(MinecraftServer server) {
        if (server==null || !server.isSameThread()) return;
        var players=server.getPlayerList().getPlayers().stream().filter(player->player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && (player.level().dimension().location()+"#map_031").equals(player.getPersistentData().getString("Blacksouls2CurrentScene"))
                && !OriginalOpeningService.managed(player) && !TurnBattleManager.isInBattle(player)
                && !OriginalNpcService.isConversing(player) && !OriginalWorldTransferService.pending(player))
                .sorted(Comparator.comparing(ServerPlayer::getUUID)).toList();
        if (players.isEmpty()) return;
        var actors=new LinkedHashMap<ServerLevel,List<EntityOriginalNpc>>();
        for (var player:players) if (!actors.containsKey(player.serverLevel())) {
            var selected=new ArrayList<EntityOriginalNpc>();
            for (var entity:player.serverLevel().getAllEntities()) if (entity instanceof EntityOriginalNpc npc
                    && isManaged(npc.getNpcId()) && npc.isAlive() && !npc.isRemoved() && !npc.isStaticMapProxy()) selected.add(npc);
            actors.put(player.serverLevel(),List.copyOf(selected));
        }
        for (var player:players) {
            var selected=actors.get(player.serverLevel());
            if (selected.size()==1) OriginalNpcService.startAutorun(player,selected.get(0));
        }
    }
}
