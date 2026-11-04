package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.story.OriginalNpcService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record ServerboundOriginalNpcPacket(int entityId, UUID sessionId, long revision, int selection) {
    public ServerboundOriginalNpcPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readUUID(), buffer.readVarLong(), buffer.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeUUID(sessionId);
        buffer.writeVarLong(revision);
        buffer.writeVarInt(selection);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && selection >= -4 && selection < 256 && revision >= 0L) {
                OriginalNpcService.respond(player, entityId, sessionId, revision, selection);
            }
        });
        context.setPacketHandled(true);
    }
}
