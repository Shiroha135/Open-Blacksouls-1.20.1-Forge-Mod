package com.BlackSouls.BlackSoulsMod.network.packets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class ClientboundAdviceVisibilityPacket {
    private final boolean controlled;
    private final boolean visible;

    public ClientboundAdviceVisibilityPacket(boolean controlled, boolean visible) {
        this.controlled = controlled;
        this.visible = visible;
    }

    public ClientboundAdviceVisibilityPacket(FriendlyByteBuf buffer) {
        controlled = buffer.readBoolean();
        visible = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(controlled);
        buffer.writeBoolean(visible);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> ClientHandler.apply(this));
    }

    private static final class ClientHandler {
        private static void apply(ClientboundAdviceVisibilityPacket packet) {
            com.BlackSouls.BlackSoulsMod.client.ClientAdviceState.set(packet.controlled, packet.visible);
        }
    }
}
