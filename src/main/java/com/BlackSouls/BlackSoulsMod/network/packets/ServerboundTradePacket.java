package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.TradeService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundTradePacket {
    private static final int MAX_ITEM_ID_LENGTH = 128;

    private final Action action;
    private final String itemRLString;
    private final int quantity;

    public ServerboundTradePacket(Action action, String itemRLString, int quantity) {
        this.action = action;
        this.itemRLString = itemRLString;
        this.quantity = quantity;
    }

    public ServerboundTradePacket(FriendlyByteBuf buf) {
        this.action = PacketHandlers.readEnum(buf, Action.values());
        this.itemRLString = buf.readUtf(MAX_ITEM_ID_LENGTH);
        this.quantity = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        PacketHandlers.writeEnum(buf, this.action);
        buf.writeUtf(this.itemRLString, MAX_ITEM_ID_LENGTH);
        buf.writeVarInt(this.quantity);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                long now = System.currentTimeMillis();
                if (now - stats.lastTradeTime < 50) {
                    return;
                }
                stats.lastTradeTime = now;

                switch (this.action) {
                    case BUY -> TradeService.handleBuy(player, this.itemRLString, this.quantity);
                    case SELL -> TradeService.handleSell(player, this.itemRLString, this.quantity);
                }
            });
        });
    }

    public enum Action {
        BUY,
        SELL
    }
}
