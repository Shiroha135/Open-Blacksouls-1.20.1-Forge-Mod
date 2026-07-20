package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketConvertSouls {
    private final long amount;

    public PacketConvertSouls(long amount) {
        this.amount = amount;
    }

    public PacketConvertSouls(FriendlyByteBuf buf) {
        this.amount = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(this.amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                    if (amount > 0 && stats.souls >= amount && stats.currentExp <= Long.MAX_VALUE - amount) {
                        stats.souls -= amount;
                        stats.addExp(amount);

                        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.0F);
                        NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);

                        player.sendSystemMessage(Component.translatable("message.blacksouls.levelup.success", stats.level));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.blacksouls.levelup.fail"));
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
