package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.render.TextBannerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundBossVictoryPacket {
    public ClientboundBossVictoryPacket() {
    }

    public ClientboundBossVictoryPacket(FriendlyByteBuf buffer) {
    }

    public void toBytes(FriendlyByteBuf buffer) {
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> TextBannerRenderer.showCentered(
                Component.literal("VICTORY  ACHIEVED"), 0xFFFFA0, 80));
    }
}
