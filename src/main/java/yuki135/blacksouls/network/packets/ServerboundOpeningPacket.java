package yuki135.blacksouls.network.packets;

import yuki135.blacksouls.story.OriginalOpeningService;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundOpeningPacket(UUID session, int revision, Action action, int value) {
    public enum Action { CONFIRM_ORIGIN, RELATIONSHIP, NEXT, SKIP, READY, DOWN, RETRY, EXIT_REPLAY, CLOSE_PREVIEW }

    public ServerboundOpeningPacket {
        java.util.Objects.requireNonNull(session);
        java.util.Objects.requireNonNull(action);
        if (revision < 0 || value < 0 || value > 9) throw new IllegalArgumentException("Invalid opening action");
    }

    public ServerboundOpeningPacket(FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readVarInt(), PacketHandlers.readEnum(buffer, Action.values()), buffer.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(session);
        buffer.writeVarInt(revision);
        PacketHandlers.writeEnum(buffer, action);
        buffer.writeVarInt(value);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        PacketHandlers.handleOpeningServer(context, event -> {
            if (event.getSender() != null) OriginalOpeningService.handle(event.getSender(), this);
        });
    }
}
