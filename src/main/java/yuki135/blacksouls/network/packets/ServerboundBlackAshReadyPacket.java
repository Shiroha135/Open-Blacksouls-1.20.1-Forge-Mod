package yuki135.blacksouls.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ServerboundBlackAshReadyPacket() {
    public ServerboundBlackAshReadyPacket(FriendlyByteBuf buffer) { this(); }

    public void toBytes(FriendlyByteBuf buffer) { }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) yuki135.blacksouls.story.OriginalBlackAshService.arrived(context.getSender());
        });
        context.setPacketHandled(true);
    }
}
