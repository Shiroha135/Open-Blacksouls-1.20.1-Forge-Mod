package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.util.OriginalSkillAcquisitionData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundOriginalSkillInfoPacket(long revision, ResourceLocation dimension, int skillId,
                                                int actorOperand, boolean learned, boolean clear) {
    public ClientboundOriginalSkillInfoPacket {
        if (revision < 1 || dimension == null || clear && (skillId != 0 || actorOperand != 0 || learned)) {
            throw new IllegalArgumentException("Invalid original skill info packet");
        }
        if (!clear) OriginalSkillAcquisitionData.validate(skillId, actorOperand);
    }

    public ClientboundOriginalSkillInfoPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarLong(), buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarLong(revision);
        buffer.writeResourceLocation(dimension);
        buffer.writeVarInt(skillId);
        buffer.writeVarInt(actorOperand);
        buffer.writeBoolean(learned);
        buffer.writeBoolean(clear);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        PacketHandlers.handleClient(context, () -> ClientHandler.apply(this));
    }

    private static final class ClientHandler {
        private static void apply(ClientboundOriginalSkillInfoPacket packet) {
            com.BlackSouls.BlackSoulsMod.client.OriginalSkillAcquisitionController.apply(packet);
        }
    }
}
