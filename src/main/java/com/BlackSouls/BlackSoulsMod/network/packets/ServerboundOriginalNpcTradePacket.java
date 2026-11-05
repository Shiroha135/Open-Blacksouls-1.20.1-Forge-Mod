package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.story.OriginalNpcService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record ServerboundOriginalNpcTradePacket(int entityId, UUID sessionId, long revision,
                                                Action action, int kind, int id, int quantity) {
    public ServerboundOriginalNpcTradePacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readUUID(), buffer.readVarLong(),
                PacketHandlers.readEnum(buffer, Action.values()), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeUUID(sessionId);
        buffer.writeVarLong(revision);
        PacketHandlers.writeEnum(buffer, action);
        buffer.writeVarInt(kind);
        buffer.writeVarInt(id);
        buffer.writeVarInt(quantity);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || action == null || revision < 0L) return;
            if (action == Action.TOGGLE_STORAGE) {
                if (kind != 0 || id != 0 || quantity != 0) return;
            } else if (kind < 0 || kind > 2 || id < 1 || quantity < 1 || quantity > 999) {
                return;
            }
            OriginalNpcService.trade(player, entityId, sessionId, revision, action, kind, id, quantity);
        });
    }

    public enum Action {
        BUY,
        SELL,
        TOGGLE_STORAGE,
        DEPOSIT,
        WITHDRAW,
        SYNTHESIZE
    }
}
