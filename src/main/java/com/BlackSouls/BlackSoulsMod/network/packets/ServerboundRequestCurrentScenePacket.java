package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundRequestCurrentScenePacket {
    private static final String CURRENT_SCENE_TAG = "Blacksouls2CurrentScene";

    public ServerboundRequestCurrentScenePacket() {
    }

    public ServerboundRequestCurrentScenePacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String stored = player.getPersistentData().getString(CURRENT_SCENE_TAG);
            int separator = stored.lastIndexOf('#');
            String sceneId = separator >= 0 && separator + 1 < stored.length()
                    ? stored.substring(separator + 1)
                    : stored;
            if (sceneId.isBlank()) {
                return;
            }
            NetworkHandler.sendToPlayer(new ClientboundCurrentScenePacket(sceneId), player);
        });
    }
}
