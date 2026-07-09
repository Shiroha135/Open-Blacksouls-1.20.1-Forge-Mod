package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.entity.EntityNoden;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundNodenRewardPacket {
    private final Type type;
    private final int entityId;

    public ServerboundNodenRewardPacket(Type type, int entityId) {
        this.type = type;
        this.entityId = entityId;
    }

    public ServerboundNodenRewardPacket(FriendlyByteBuf buf) {
        this.type = PacketHandlers.readEnum(buf, Type.values());
        this.entityId = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        PacketHandlers.writeEnum(buf, this.type);
        buf.writeVarInt(this.entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            Entity entity = player.level().getEntity(this.entityId);
            if (!(entity instanceof EntityNoden) || entity.distanceToSqr(player) > 64.0D) {
                return;
            }

            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                int delta = this.type.senDelta;
                stats.sen = Math.max(0, stats.sen + delta);
                StatEventHandler.syncToClient(player);
                NetworkHandler.sendToPlayer(new ClientboundBannerPacket(ClientboundBannerPacket.Type.SEEK_SERVICE, delta), player);
            });
        });
    }

    public enum Type {
        KISS(-1),
        SEEK_SERVICE(10);

        private final int senDelta;

        Type(int senDelta) {
            this.senDelta = senDelta;
        }
    }
}
