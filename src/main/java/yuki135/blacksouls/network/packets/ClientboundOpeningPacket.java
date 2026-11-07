package yuki135.blacksouls.network.packets;

import yuki135.blacksouls.story.OriginalOpeningProfile.Stage;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundOpeningPacket(UUID session, int revision, ResourceLocation dimension, BlockPos anchor,
                                      Stage stage, boolean preview, boolean locked, boolean awaitingReady,
                                      int focusOrigin, int relationship, int command, int waitTicks, String notice) {
    public ClientboundOpeningPacket {
        java.util.Objects.requireNonNull(session);
        java.util.Objects.requireNonNull(dimension);
        java.util.Objects.requireNonNull(anchor);
        java.util.Objects.requireNonNull(stage);
        anchor = anchor.immutable();
        if (revision < 0 || focusOrigin < 0 || focusOrigin > 9 || relationship < 0 || relationship > 3
                || command < 0 || command > 1_000_000 || waitTicks < 0 || waitTicks > 1_000_000
                || notice == null || notice.length() > 512) throw new IllegalArgumentException("Invalid opening presentation");
    }

    public ClientboundOpeningPacket(FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readVarInt(), buffer.readResourceLocation(), buffer.readBlockPos(),
                PacketHandlers.readEnum(buffer, Stage.values()), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(512));
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(session);
        buffer.writeVarInt(revision);
        buffer.writeResourceLocation(dimension);
        buffer.writeBlockPos(anchor);
        PacketHandlers.writeEnum(buffer, stage);
        buffer.writeBoolean(preview);
        buffer.writeBoolean(locked);
        buffer.writeBoolean(awaitingReady);
        buffer.writeVarInt(focusOrigin);
        buffer.writeVarInt(relationship);
        buffer.writeVarInt(command);
        buffer.writeVarInt(waitTicks);
        buffer.writeUtf(notice, 512);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        PacketHandlers.handleClient(context, () -> ClientHandler.apply(this));
    }

    private static final class ClientHandler {
        private static void apply(ClientboundOpeningPacket packet) {
            yuki135.blacksouls.client.OriginalOpeningController.apply(packet);
        }
    }
}
