package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.handler.FogGateTraversalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundFogGateProceedPacket {
    private final BlockPos gatePos;

    public ServerboundFogGateProceedPacket(BlockPos gatePos) {
        this.gatePos = gatePos.immutable();
    }

    public ServerboundFogGateProceedPacket(FriendlyByteBuf buf) {
        this.gatePos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(gatePos);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                FogGateTraversalHandler.start(player, gatePos);
            }
        });
    }
}
