package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.render.TextBannerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundTextBannerPacket {
    private final Component message;

    public ClientboundTextBannerPacket(Component message) {
        this.message = message;
    }

    public ClientboundTextBannerPacket(FriendlyByteBuf buffer) {
        this.message = buffer.readComponent();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeComponent(message);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> TextBannerRenderer.show(message));
    }
}
