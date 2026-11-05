package com.BlackSouls.BlackSoulsMod.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalWhiteBearPlacement;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundWhiteBearActionPacket {
    private final Action action;

    public ServerboundWhiteBearActionPacket(Action action) {
        this.action = action;
    }

    public ServerboundWhiteBearActionPacket(FriendlyByteBuf buf) {
        this.action = PacketHandlers.readEnum(buf, Action.values());
    }

    public void toBytes(FriendlyByteBuf buf) {
        PacketHandlers.writeEnum(buf, this.action);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || this.action == null) {
                return;
            }
            if (this.action == Action.STORAGE) {
                OriginalWhiteBearPlacement.interact(player);
            }
        });
    }

    public enum Action {
        STORAGE
    }
}
