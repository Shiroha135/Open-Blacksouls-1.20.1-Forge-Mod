package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.entity.EntityCheshireCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundCheshireGiftPacket {
    private final int entityId;
    private final int primaryGift;
    private final int secondaryGift;

    public ServerboundCheshireGiftPacket(int entityId, int primaryGift, int secondaryGift) {
        this.entityId = entityId;
        this.primaryGift = primaryGift;
        this.secondaryGift = secondaryGift;
    }

    public ServerboundCheshireGiftPacket(FriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        primaryGift = buffer.readVarInt();
        secondaryGift = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(primaryGift);
        buffer.writeVarInt(secondaryGift);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            Entity target = player.level().getEntity(entityId);
            if (target instanceof EntityCheshireCat cheshire && cheshire.isAlive()
                    && !cheshire.isRemoved() && player.distanceToSqr(cheshire) <= 64.0D) {
                cheshire.giveRabbitHoleGift(player, primaryGift, secondaryGift);
            }
        });
    }
}
