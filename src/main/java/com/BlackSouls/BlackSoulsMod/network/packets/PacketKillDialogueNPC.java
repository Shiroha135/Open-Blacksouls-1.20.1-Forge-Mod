package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.entity.EntityNoden;
import com.BlackSouls.BlackSoulsMod.entity.EntityRabbitHoleNpc;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketKillDialogueNPC {
    private final int entityId;

    public PacketKillDialogueNPC(int entityId) {
        this.entityId = entityId;
    }

    public PacketKillDialogueNPC(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            Level level = player.level();
            Entity targetEntity = level.getEntity(this.entityId);

            if (targetEntity instanceof EntityRabbitHoleNpc rabbit && rabbit.isAlive() && !rabbit.isRemoved()
                    && rabbit.distanceToSqr(player) <= 64.0D) {
                rabbit.startKillBattle(player);
                return;
            }

            if (targetEntity instanceof EntityNoden noden && noden.isAlive() && !noden.isRemoved()
                    && noden.distanceToSqr(player) <= 64.0D) {
                noden.discard();
                player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                    stats.sen = Math.max(0, stats.sen - 30);
                    StatEventHandler.syncToClient(player);
                    NetworkHandler.sendToPlayer(new ClientboundBannerPacket(ClientboundBannerPacket.Type.SEEK_SERVICE, -30), player);
                });
            }
        });
        context.setPacketHandled(true);
    }
}
