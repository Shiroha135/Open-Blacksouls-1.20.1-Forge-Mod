package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import com.BlackSouls.BlackSoulsMod.util.OriginalWorldEventData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class OriginalWorldTransferService {
    public static final int OUT_FRAMES = 30;
    public static final int IN_START_FRAME = 45;
    public static final int TOTAL_FRAMES = 75;

    private OriginalWorldTransferService() {
    }

    public static int begin(ServerPlayer player, EntityOriginalNpc source, Vec3 destination, float yaw, int fade) {
        if (!validSource(player, source) || !Float.isFinite(yaw) || fade != 0 && fade != 2
                || pending(player) || !validDestination(player, destination)) throw new UnsupportedOperationException("original_world_transfer_context");
        if (fade == 2) {
            transfer(player, destination, yaw);
            OriginalPoolCrowdVisitService.transferred(player, source.getNpcId(), destination);
            return 0;
        }
        var data = OriginalWorldTransferData.get(player.server);
        data.tickets.put(player.getUUID(), new OriginalWorldTransferData.Ticket(player.getUUID(), source.getUUID(), source.getNpcId(),
                player.level().dimension().location().toString(), player.position(), destination, yaw));
        data.setDirty();
        present(player, source, 221);
        return TOTAL_FRAMES;
    }

    public static boolean pending(ServerPlayer player) {
        return OriginalWorldTransferData.get(player.server).tickets.containsKey(player.getUUID());
    }

    public static void restorePresentation(ServerPlayer player) {
        var ticket = OriginalWorldTransferData.get(player.server).tickets.get(player.getUUID());
        if (ticket == null || !player.level().dimension().location().toString().equals(ticket.dimension)
                || !(player.serverLevel().getEntity(ticket.source) instanceof EntityOriginalNpc source)
                || !source.isAlive() || !source.getNpcId().equals(ticket.sourceEvent)) return;
        int code = ticket.elapsed < IN_START_FRAME ? 221 : 222;
        int elapsed = ticket.elapsed < IN_START_FRAME ? Math.min(OUT_FRAMES, ticket.elapsed) : ticket.elapsed - IN_START_FRAME;
        var packet = OriginalNpcPresentationService.createPacket(player, source, code, List.of());
        NetworkHandler.sendToPlayer(new com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPresentationPacket(
                code, packet.entityId(), packet.dimension(), List.of(2.0D, (double) elapsed), packet.sound()), player);
    }

    public static void tick(MinecraftServer server) {
        var data = OriginalWorldTransferData.get(server);
        int tick = server.getTickCount();
        if (data.lastServerTick == tick) return;
        data.lastServerTick = tick;
        for (var owner : List.copyOf(data.tickets.keySet())) {
            var ticket = data.tickets.get(owner);
            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (player == null || !player.isAlive() || player.isSpectator() || TurnBattleManager.isInBattle(player)
                    || !player.level().dimension().location().toString().equals(ticket.dimension)
                    || OriginalNpcService.isPictureClockSuspended(player)
                    || !(player.serverLevel().getEntity(ticket.source) instanceof EntityOriginalNpc source)
                    || !source.isAlive() || source.isRemoved() || !source.getNpcId().equals(ticket.sourceEvent)) continue;
            for (int frame = 0; frame < 3; frame++) {
                if (!ticket.transferred && ticket.elapsed + 1 >= OUT_FRAMES && !validDestination(player, ticket.destination)) break;
                ticket.elapsed++;
                if (!ticket.transferred && ticket.elapsed == OUT_FRAMES) {
                    transfer(player, ticket.destination, ticket.yaw);
                    OriginalPoolCrowdVisitService.transferred(player, ticket.sourceEvent, ticket.destination);
                    ticket.transferred = true;
                }
                if (!ticket.fadeInSent && ticket.elapsed == IN_START_FRAME) {
                    present(player, source, 222);
                    ticket.fadeInSent = true;
                }
                if (ticket.elapsed == TOTAL_FRAMES) {
                    data.tickets.remove(owner);
                    break;
                }
            }
            data.setDirty();
        }
    }

    private static boolean validSource(ServerPlayer player, EntityOriginalNpc source) {
        return player != null && source != null && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && !TurnBattleManager.isInBattle(player) && source.isAlive() && !source.isRemoved()
                && player.level() == source.level() && HokoniwaDestination.DIMENSION.equals(player.level().dimension())
                && player.serverLevel().getEntity(source.getUUID()) == source
                && OriginalWorldEventData.isNativeBlock(source.getNpcId()) && player.distanceToSqr(source) <= 64.0D;
    }

    private static boolean validDestination(ServerPlayer player, Vec3 destination) {
        if (destination == null || !OriginalWorldTransferData.coordinate(destination.x)
                || !OriginalWorldTransferData.coordinate(destination.y) || !OriginalWorldTransferData.coordinate(destination.z)) return false;
        ServerLevel level = player.serverLevel();
        BlockPos feet = BlockPos.containing(destination), floor = BlockPos.containing(destination.add(0, -0.02D, 0));
        var bounds = player.getBoundingBox().move(destination.subtract(player.position()));
        return feet.getY() >= level.getMinBuildHeight() && feet.getY() + 2 < level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(feet) && level.hasChunkAt(feet) && level.hasChunkAt(feet.above(2))
                && (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                || OriginalDodoEntry.supportsShapedArrival(player, destination, bounds))
                && level.noCollision(player, bounds);
    }

    private static void transfer(ServerPlayer player, Vec3 destination, float yaw) {
        player.stopRiding();
        OriginalSkillAcquisitionService.clear(player);
        player.connection.teleport(destination.x, destination.y, destination.z, yaw, player.getXRot());
        OriginalNpcSymbolService.synchronizeStealthAnchor(player);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.setOnGround(true);
    }

    private static void present(ServerPlayer player, EntityOriginalNpc source, int code) {
        var packet = OriginalNpcPresentationService.createPacket(player, source, code, List.of());
        NetworkHandler.sendToPlayer(new com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPresentationPacket(
                packet.commandCode(), packet.entityId(), packet.dimension(), List.of(1.0D), packet.sound()), player);
    }
}
