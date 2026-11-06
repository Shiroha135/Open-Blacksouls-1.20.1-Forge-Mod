package yuki135.blacksouls.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public record ClientboundOriginalNpcAnimationPacket(UUID playbackId, int entityId, ResourceLocation dimension,
                                                    int animationId, int elapsedFrames, int position,
                                                    double x, double y, double z,
                                                    boolean newPlayback, boolean paused) {
    public ClientboundOriginalNpcAnimationPacket {
        Objects.requireNonNull(playbackId);
        Objects.requireNonNull(dimension);
        if (animationId <= 0 || elapsedFrames < 0 || elapsedFrames > 360000 || position < 0 || position > 3
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Invalid original NPC animation packet");
        }
    }

    public ClientboundOriginalNpcAnimationPacket(FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readVarInt(), buffer.readResourceLocation(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readBoolean(), buffer.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(playbackId);
        buffer.writeVarInt(entityId);
        buffer.writeResourceLocation(dimension);
        buffer.writeVarInt(animationId);
        buffer.writeVarInt(elapsedFrames);
        buffer.writeVarInt(position);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeBoolean(newPlayback);
        buffer.writeBoolean(paused);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        PacketHandlers.handleClient(context, () -> ClientHandler.apply(this));
    }

    private static final class ClientHandler {
        private static void apply(ClientboundOriginalNpcAnimationPacket packet) {
            yuki135.blacksouls.client.OriginalNpcAnimationController.apply(packet);
        }
    }
}
