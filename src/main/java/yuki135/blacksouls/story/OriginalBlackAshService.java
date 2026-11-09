package yuki135.blacksouls.story;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundBlackAshPacket;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcAnimationPacket;
import yuki135.blacksouls.util.BSOriginalAnimationData;
import yuki135.blacksouls.util.HokoniwaDestination;

@Mod.EventBusSubscriber(modid="blacksouls")
public final class OriginalBlackAshService {
    private static final String HIDDEN = "BSBlackAshHidden";
    private static final String WAS_INVISIBLE = "BSBlackAshWasInvisible";
    private static final Map<MinecraftServer, Map<UUID, Transfer>> TRANSFERS = new WeakHashMap<>();

    private static final class Transfer {
        final ResourceKey<Level> source;
        final Vec3 origin;
        final float yaw;
        final float pitch;
        int ticks;
        boolean transferred;
        int arrivalTick = -1;

        Transfer(ServerPlayer player) {
            source = player.level().dimension();
            origin = player.position();
            yaw = player.getYRot();
            pitch = player.getXRot();
        }
    }

    private OriginalBlackAshService() { }

    public static boolean pending(ServerPlayer player) {
        return TRANSFERS.getOrDefault(player.server, Map.of()).containsKey(player.getUUID());
    }

    public static boolean begin(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || pending(player) || TurnBattleManager.isInBattle(player)
                || OriginalOpeningService.managed(player) || OriginalNpcService.isConversing(player)
                || OriginalWorldTransferService.pending(player) || OriginalDodoRunService.pending(player)) return false;
        if (OriginalNpcProgress.readSwitch(player, 675) || OriginalNpcProgress.readSwitch(player, 518)) {
            player.sendSystemMessage(Component.literal(OriginalNpcProgress.readSwitch(player, 675)
                    ? "似乎无法在这里使用。" : "爱丽丝就在那儿，已经没有回去的必要了。"));
            return false;
        }
        ServerLevel destination = player.server.getLevel(HokoniwaDestination.DIMENSION);
        if (destination == null || player.getCapability(BSPlayerStats.CAPABILITY).resolve().isEmpty()
                || !HokoniwaDestination.isLandingSafe(destination)) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.library_unavailable"));
            return false;
        }
        player.stopRiding();
        var transfer = new Transfer(player);
        TRANSFERS.computeIfAbsent(player.server, ignored -> new HashMap<>()).put(player.getUUID(), transfer);
        player.getPersistentData().putBoolean(WAS_INVISIBLE, player.isInvisible());
        player.getPersistentData().putBoolean(HIDDEN, true);
        player.setInvisible(true);
        player.setDeltaMovement(Vec3.ZERO);
        NetworkHandler.sendToPlayer(new ClientboundBlackAshPacket(0, player.level().dimension().location()), player);
        animation(player);
        return true;
    }

    private static void animation(ServerPlayer player) {
        var definition = BSOriginalAnimationData.get(77);
        if (definition == null) return;
        NetworkHandler.sendToAllAround(new ClientboundOriginalNpcAnimationPacket(UUID.randomUUID(), player.getId(),
                player.level().dimension().location(), 77, 0, definition.position(),
                player.getX(), player.getY(), player.getZ(), true, false), player);
    }

    public static void arrived(ServerPlayer player) {
        var transfer = TRANSFERS.getOrDefault(player.server, Map.of()).get(player.getUUID());
        if (transfer == null || !transfer.transferred || transfer.arrivalTick >= 0 || !player.isAlive()
                || !player.level().dimension().equals(HokoniwaDestination.DIMENSION)
                || player.position().distanceToSqr(new Vec3(HokoniwaDestination.X, HokoniwaDestination.Y, HokoniwaDestination.Z)) > 1D) return;
        transfer.arrivalTick = transfer.ticks;
        restoreVisibility(player);
        NetworkHandler.sendToPlayer(new ClientboundBlackAshPacket(3, player.level().dimension().location()), player);
        animation(player);
    }

    private static void restoreVisibility(ServerPlayer player) {
        var tag = player.getPersistentData();
        if (tag.getBoolean(HIDDEN)) player.setInvisible(tag.getBoolean(WAS_INVISIBLE));
        tag.remove(HIDDEN);
        tag.remove(WAS_INVISIBLE);
    }

    private static void cancel(ServerPlayer player) {
        var transfers = TRANSFERS.get(player.server);
        if (transfers != null) transfers.remove(player.getUUID());
        restoreVisibility(player);
        NetworkHandler.sendToPlayer(new ClientboundBlackAshPacket(2, player.level().dimension().location()), player);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        var transfers = TRANSFERS.get(server);
        if (transfers == null) return;
        for (var id : List.copyOf(transfers.keySet())) {
            var player = server.getPlayerList().getPlayer(id);
            var transfer = transfers.get(id);
            if (player == null) { transfers.remove(id); continue; }
            ResourceKey<Level> expected = transfer.transferred ? HokoniwaDestination.DIMENSION : transfer.source;
            if (!player.isAlive() || player.isSpectator() || !player.level().dimension().equals(expected)
                    || TurnBattleManager.isInBattle(player)) { cancel(player); continue; }
            Vec3 anchor = transfer.transferred ? new Vec3(HokoniwaDestination.X, HokoniwaDestination.Y, HokoniwaDestination.Z) : transfer.origin;
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
            if (player.position().distanceToSqr(anchor) > 0.0001D) player.connection.teleport(anchor.x, anchor.y, anchor.z,
                    transfer.transferred ? HokoniwaDestination.YAW : transfer.yaw, transfer.pitch);
            transfer.ticks++;
            if (transfer.ticks == 30) {
                ServerLevel destination = server.getLevel(HokoniwaDestination.DIMENSION);
                var stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
                if (destination == null || stats == null || !HokoniwaDestination.isLandingSafe(destination)) {
                    cancel(player);
                    player.sendSystemMessage(Component.translatable("message.blacksouls.library_unavailable"));
                    continue;
                }
                stats.souls = 0;
                player.teleportTo(destination, HokoniwaDestination.X, HokoniwaDestination.Y, HokoniwaDestination.Z,
                        HokoniwaDestination.YAW, 0);
                transfer.transferred = true;
                OriginalOpeningBonfireFlow.visitLibrary(player);
                StatEventHandler.syncToClient(player);
                NetworkHandler.sendToPlayer(new ClientboundBlackAshPacket(1, destination.dimension().location()), player);
            }
            if (transfer.arrivalTick >= 0 && transfer.ticks - transfer.arrivalTick >= 30) transfers.remove(id);
            else if (transfer.ticks > 600) cancel(player);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) restoreVisibility(player);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) if (pending(player)) cancel(player);
        TRANSFERS.remove(event.getServer());
    }
}
