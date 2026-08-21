package com.BlackSouls.BlackSoulsMod.network.packets;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundFogGateWalkPacket {
    private final Direction direction;
    private final boolean walking;
    private final double targetX;
    private final double targetZ;

    public ClientboundFogGateWalkPacket(Direction direction, double targetX, double targetZ) {
        this.direction = direction;
        this.walking = true;
        this.targetX = targetX;
        this.targetZ = targetZ;
    }

    public ClientboundFogGateWalkPacket(Direction direction) {
        this.direction = direction;
        this.walking = false;
        this.targetX = 0.0D;
        this.targetZ = 0.0D;
    }

    public ClientboundFogGateWalkPacket(FriendlyByteBuf buf) {
        this.walking = buf.readBoolean();
        this.direction = Direction.from2DDataValue(buf.readVarInt());
        this.targetX = walking ? buf.readDouble() : 0.0D;
        this.targetZ = walking ? buf.readDouble() : 0.0D;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(walking);
        buf.writeVarInt(direction.get2DDataValue());
        if (walking) {
            buf.writeDouble(targetX);
            buf.writeDouble(targetZ);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> ClientHandler.apply(this));
    }

    private static final class ClientHandler {
        private static void apply(ClientboundFogGateWalkPacket packet) {
            if (packet.walking) {
                com.BlackSouls.BlackSoulsMod.client.ClientFogGateTraversal.start(
                        packet.direction, packet.targetX, packet.targetZ
                );
            } else {
                com.BlackSouls.BlackSoulsMod.client.ClientFogGateTraversal.stop();
            }
        }
    }
}
