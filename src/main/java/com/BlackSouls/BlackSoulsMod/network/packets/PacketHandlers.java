package com.BlackSouls.BlackSoulsMod.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class PacketHandlers {

    private PacketHandlers() {
    }

    static <E extends Enum<E>> E readEnum(FriendlyByteBuf buf, E[] values) {
        int ordinal = buf.readVarInt();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }

    static void writeEnum(FriendlyByteBuf buf, Enum<?> value) {
        buf.writeVarInt(value.ordinal());
    }

    static void handleClient(Supplier<NetworkEvent.Context> supplier, Runnable task) {
        NetworkEvent.Context context = supplier.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> task));
        context.setPacketHandled(true);
    }

    static void handleServer(Supplier<NetworkEvent.Context> supplier, Consumer<NetworkEvent.Context> task) {
        NetworkEvent.Context context = supplier.get();
        if (!context.getDirection().getReceptionSide().isServer()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> task.accept(context));
        context.setPacketHandled(true);
    }
}
