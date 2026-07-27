package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundTurnBattleActionPacket {
    private final Action action;
    private final int selection;
    private final int target;

    public ServerboundTurnBattleActionPacket(Action action, int selection) {
        this(action, selection, 0);
    }

    public ServerboundTurnBattleActionPacket(Action action, int selection, int target) {
        this.action = action;
        this.selection = selection;
        this.target = target;
    }

    public ServerboundTurnBattleActionPacket(FriendlyByteBuf buf) {
        this.action = PacketHandlers.readEnum(buf, Action.values());
        this.selection = buf.readVarInt();
        this.target = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        PacketHandlers.writeEnum(buf, this.action);
        buf.writeVarInt(this.selection);
        buf.writeVarInt(this.target);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player != null && this.action != null) {
                TurnBattleManager.handleAction(player, this.action, this.selection, this.target);
            }
        });
    }

    public enum Action {
        ATTACK,
        SKILL,
        ITEM,
        GUARD,
        ESCAPE,
        WEAPON_CHANGE
    }
}
