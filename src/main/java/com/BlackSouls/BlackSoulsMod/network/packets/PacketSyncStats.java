package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketSyncStats {
    private final CompoundTag nbt;

    public PacketSyncStats(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public PacketSyncStats(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        ctx.setPacketHandled(true);
    }

    private static class ClientHandler {
        public static void handle(PacketSyncStats msg) {
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
                if (stats != null && msg.nbt != null) {
                    stats.deserializeNBT(msg.nbt);
                    StatEventHandler.applyStats(player);
                }
            }
        }
    }
}
