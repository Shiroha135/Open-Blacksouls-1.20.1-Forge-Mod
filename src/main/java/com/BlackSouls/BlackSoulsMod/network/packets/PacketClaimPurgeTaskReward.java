package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketClaimPurgeTaskReward {
    private static final int MAX_TASKS = 10;

    private final int taskIndex;

    public PacketClaimPurgeTaskReward(int taskIndex) {
        this.taskIndex = taskIndex;
    }

    public PacketClaimPurgeTaskReward(FriendlyByteBuf buf) {
        this.taskIndex = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.taskIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && this.taskIndex >= 0 && this.taskIndex < MAX_TASKS) {
                StatEventHandler.claimPurgeTaskReward(player, this.taskIndex);
            }
        });
        context.setPacketHandled(true);
    }
}
