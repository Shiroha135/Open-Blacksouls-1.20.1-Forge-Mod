package yuki135.blacksouls.network.packets;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundFogGateWalkPacket {
    private final Direction direction;
    private final boolean walking;
    private final double targetX;
    private final double targetZ;
    private final boolean previousNoPhysics;
    private final boolean previousNoGravity;

    public ClientboundFogGateWalkPacket(Direction direction, double targetX, double targetZ,
                                       boolean previousNoPhysics, boolean previousNoGravity) {
        this.direction = direction;
        this.walking = true;
        this.targetX = targetX;
        this.targetZ = targetZ;
        this.previousNoPhysics = previousNoPhysics;
        this.previousNoGravity = previousNoGravity;
    }

    public ClientboundFogGateWalkPacket(Direction direction, boolean previousNoPhysics, boolean previousNoGravity) {
        this.direction = direction;
        this.walking = false;
        this.targetX = 0.0D;
        this.targetZ = 0.0D;
        this.previousNoPhysics = previousNoPhysics;
        this.previousNoGravity = previousNoGravity;
    }

    public ClientboundFogGateWalkPacket(FriendlyByteBuf buf) {
        this.walking = buf.readBoolean();
        this.direction = Direction.from2DDataValue(buf.readVarInt());
        this.targetX = walking ? buf.readDouble() : 0.0D;
        this.targetZ = walking ? buf.readDouble() : 0.0D;
        this.previousNoPhysics = buf.readBoolean();
        this.previousNoGravity = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(walking);
        buf.writeVarInt(direction.get2DDataValue());
        if (walking) {
            buf.writeDouble(targetX);
            buf.writeDouble(targetZ);
        }
        buf.writeBoolean(previousNoPhysics);
        buf.writeBoolean(previousNoGravity);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> ClientHandler.apply(this));
    }

    private static final class ClientHandler {
        private static void apply(ClientboundFogGateWalkPacket packet) {
            if (packet.walking) {
                yuki135.blacksouls.client.ClientFogGateTraversal.start(
                        packet.direction, packet.targetX, packet.targetZ, packet.previousNoPhysics, packet.previousNoGravity
                );
            } else {
                yuki135.blacksouls.client.ClientFogGateTraversal.stop(packet.previousNoPhysics, packet.previousNoGravity);
            }
        }
    }
}
