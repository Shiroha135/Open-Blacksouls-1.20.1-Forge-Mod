package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.OriginalNpcPresentationController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ClientboundOriginalNpcPresentationPacket(int commandCode, int entityId,
                                                      ResourceLocation dimension, List<Double> values, String sound) {
    public ClientboundOriginalNpcPresentationPacket {
        values = List.copyOf(values);
        if (values.size() > 8 || values.stream().anyMatch(value -> !Double.isFinite(value))) {
            throw new IllegalArgumentException("Invalid original NPC presentation values");
        }
        sound = sound == null ? "" : sound;
    }

    public ClientboundOriginalNpcPresentationPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt(), buffer.readResourceLocation(), readValues(buffer), buffer.readUtf(512));
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(commandCode);
        buffer.writeVarInt(entityId);
        buffer.writeResourceLocation(dimension);
        buffer.writeVarInt(values.size());
        values.forEach(buffer::writeDouble);
        buffer.writeUtf(sound, 512);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        PacketHandlers.handleClient(context, () -> ClientHandler.apply(this));
    }

    private static List<Double> readValues(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 8) {
            throw new IllegalArgumentException("Invalid original NPC presentation value count");
        }
        List<Double> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(buffer.readDouble());
        }
        return result;
    }

    private static final class ClientHandler {
        private static void apply(ClientboundOriginalNpcPresentationPacket packet) {
            OriginalNpcPresentationController.apply(packet);
        }
    }
}
