package yuki135.blacksouls.network.packets;

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
        PacketHandlers.handleClient(supplier, () -> ClientHandler.open(this));
    }

    private static final class ClientHandler {
        private static void open(ClientboundOpenFogGatePromptPacket packet) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new yuki135.blacksouls.client.gui.GuiFogGatePrompt(packet.gatePos)
            );
        }
    }
}
