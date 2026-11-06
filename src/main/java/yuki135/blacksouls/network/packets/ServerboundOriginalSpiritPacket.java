package yuki135.blacksouls.network.packets;

import yuki135.blacksouls.story.OriginalSpiritService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ServerboundOriginalSpiritPacket(int spiritId) {
    public ServerboundOriginalSpiritPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(spiritId);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && spiritId >= 0 && spiritId <= 68) OriginalSpiritService.select(player, spiritId);
        });
        context.setPacketHandled(true);
    }
}
