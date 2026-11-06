package com.BlackSouls.BlackSoulsMod.network.packets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundOriginalSenInfoPacket(long revision, ResourceLocation dimension, int current,
                                              long delta, boolean clear) {
    public ClientboundOriginalSenInfoPacket {
        if (revision < 1 || dimension == null || (clear ? current != 0 || delta != 0 : delta == 0)) {
            throw new IllegalArgumentException("Invalid original SEN info packet");
        }
    }

    public ClientboundOriginalSenInfoPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarLong(), buffer.readResourceLocation(), buffer.readVarInt(), buffer.readLong(), buffer.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarLong(revision);
        buffer.writeResourceLocation(dimension);
        buffer.writeVarInt(current);
        buffer.writeLong(delta);
        buffer.writeBoolean(clear);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        PacketHandlers.handleClient(context, () -> ClientHandler.apply(this));
    }

    private static final class ClientHandler {
        private static void apply(ClientboundOriginalSenInfoPacket packet) {
            com.BlackSouls.BlackSoulsMod.client.OriginalSkillAcquisitionController.apply(packet);
        }
    }
}
