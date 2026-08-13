package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.party.PartyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public final class ServerboundPartySyncPacket {
    private final String avatar;
    public ServerboundPartySyncPacket(String avatar) { this.avatar = avatar; }
    public ServerboundPartySyncPacket(FriendlyByteBuf buf) { this.avatar = buf.readUtf(64); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeUtf(this.avatar, 64); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player != null) PartyManager.syncFor(player, this.avatar);
        });
    }
}
