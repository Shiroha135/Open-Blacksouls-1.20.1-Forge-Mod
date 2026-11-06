package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalNpcConversationMovement {
    private static final Map<UUID, EntityOriginalNpc> LOCKED = new HashMap<>();

    private OriginalNpcConversationMovement() {
    }

    public static void start(ServerPlayer player, PathfinderMob actor) {
        if (!(actor instanceof EntityOriginalNpc npc) || !npc.hasOriginalAutonomy()) return;
        EntityOriginalNpc previous = LOCKED.put(player.getUUID(), npc);
        if (previous != null && previous != npc) previous.unlockOriginalMovement();
        npc.lockOriginalMovement(player);
    }

    public static void release(UUID owner) {
        EntityOriginalNpc actor = LOCKED.remove(owner);
        if (actor != null) actor.unlockOriginalMovement();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (UUID owner : java.util.List.copyOf(LOCKED.keySet())) {
            EntityOriginalNpc actor = LOCKED.get(owner);
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(owner);
            if (player == null || !player.isAlive() || player.level() != actor.level() || !actor.isAlive() || actor.isRemoved()
                    || !OriginalNpcService.isConversationActor(owner, actor)) release(owner);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        release(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        for (UUID owner : java.util.List.copyOf(LOCKED.keySet())) release(owner);
    }
}
