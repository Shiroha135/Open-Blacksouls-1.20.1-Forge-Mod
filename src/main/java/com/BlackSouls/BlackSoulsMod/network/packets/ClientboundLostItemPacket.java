package com.BlackSouls.BlackSoulsMod.network.packets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundLostItemPacket {
    private final ItemStack stack;

    public ClientboundLostItemPacket(ItemStack stack) {
        this.stack = stack.copy();
    }

    public ClientboundLostItemPacket(FriendlyByteBuf buffer) {
        this.stack = buffer.readItem();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeItem(this.stack);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> ClientHandler.show(this));
    }

    private static final class ClientHandler {
        private static void show(ClientboundLostItemPacket packet) {
            com.BlackSouls.BlackSoulsMod.client.render.LostItemBannerRenderer.show(packet.stack);
        }
    }
}
