package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class ServerboundTurnBattlePresentationPacket {
    public ServerboundTurnBattlePresentationPacket() {
    }

    public ServerboundTurnBattlePresentationPacket(FriendlyByteBuf buffer) {
    }

    public void toBytes(FriendlyByteBuf buffer) {
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                TurnBattleManager.handlePresentationComplete(player);
            }
        });
    }
}
