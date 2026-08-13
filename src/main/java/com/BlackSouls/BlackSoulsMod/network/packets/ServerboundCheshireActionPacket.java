package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.entity.EntityCheshireCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundCheshireActionPacket {
    private final int entityId;
    private final Action action;

    public ServerboundCheshireActionPacket(int entityId, Action action) {
        this.entityId = entityId;
        this.action = action;
    }

    public ServerboundCheshireActionPacket(FriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        action = PacketHandlers.readEnum(buffer, Action.values());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        PacketHandlers.writeEnum(buffer, action);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || action == null) {
                return;
            }
            Entity target = player.level().getEntity(entityId);
            if (!(target instanceof EntityCheshireCat cheshire) || !cheshire.isAlive()
                    || cheshire.isRemoved() || player.distanceToSqr(cheshire) > 64.0D) {
                return;
            }
            if (action == Action.ASK_ALICE) {
                cheshire.finishAliceQuestion(player);
            } else {
                cheshire.vanishFromThreat(player);
            }
        });
    }

    public enum Action {
        ASK_ALICE,
        VANISH
    }
}
