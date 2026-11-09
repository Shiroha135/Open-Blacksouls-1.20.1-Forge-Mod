package yuki135.blacksouls.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ClientboundBlackAshPacket(int phase, ResourceLocation dimension) {
    public ClientboundBlackAshPacket {
        if (phase < 0 || phase > 3 || dimension == null) throw new IllegalArgumentException("Invalid black ash presentation");
    }

    public ClientboundBlackAshPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readResourceLocation());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(phase);
        buffer.writeResourceLocation(dimension);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        PacketHandlers.handleClient(context, () -> ClientHandler.apply(this));
    }

    private static final class ClientHandler {
        private static void apply(ClientboundBlackAshPacket packet) {
            yuki135.blacksouls.client.BlackAshPresentationController.apply(packet);
        }
    }
}
