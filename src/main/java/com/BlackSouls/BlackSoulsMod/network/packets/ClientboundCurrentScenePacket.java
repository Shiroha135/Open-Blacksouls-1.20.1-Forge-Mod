package com.BlackSouls.BlackSoulsMod.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundCurrentScenePacket {
    private final String sceneId;

    public ClientboundCurrentScenePacket(String sceneId) {
        this.sceneId = sceneId == null ? "" : sceneId;
    }

    public ClientboundCurrentScenePacket(FriendlyByteBuf buf) {
        sceneId = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(sceneId, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> ClientHandler.apply(this));
    }

    private static final class ClientHandler {
        private static void apply(ClientboundCurrentScenePacket packet) {
            com.BlackSouls.BlackSoulsMod.client.ClientSceneState.set(packet.sceneId);
        }
    }
}
