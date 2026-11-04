package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import java.util.function.Supplier;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class ServerboundTurnBattlePresentationPacket {
    private final UUID troopToken;
    private final long revision;
    private final int selection;

    public ServerboundTurnBattlePresentationPacket() {
        this(null, 0L, -1);
    }

    public ServerboundTurnBattlePresentationPacket(UUID troopToken, long revision, int selection) {
        this.troopToken = troopToken;
        this.revision = revision;
        this.selection = selection;
    }

    public ServerboundTurnBattlePresentationPacket(FriendlyByteBuf buffer) {
        this.troopToken = buffer.readableBytes() > 0 && buffer.readBoolean() ? buffer.readUUID() : null;
        this.revision = this.troopToken == null ? 0L : buffer.readVarLong();
        this.selection = this.troopToken == null ? -1 : buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.troopToken != null);
        if (this.troopToken != null) {
            buffer.writeUUID(this.troopToken);
            buffer.writeVarLong(this.revision);
            buffer.writeVarInt(this.selection);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                if (this.troopToken == null) {
                    TurnBattleManager.handlePresentationComplete(player);
                } else {
                    TurnBattleManager.handleTroopPresentation(player, this.troopToken, this.revision, this.selection);
                }
            }
        });
    }
}
