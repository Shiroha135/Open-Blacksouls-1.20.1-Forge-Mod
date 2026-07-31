package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.gui.GuiFogGatePrompt;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundOpenFogGatePromptPacket {
    private final BlockPos gatePos;

    public ClientboundOpenFogGatePromptPacket(BlockPos gatePos) {
        this.gatePos = gatePos.immutable();
    }

    public ClientboundOpenFogGatePromptPacket(FriendlyByteBuf buf) {
        this.gatePos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(gatePos);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () ->
                Minecraft.getInstance().setScreen(new GuiFogGatePrompt(gatePos))
        );
    }
}
