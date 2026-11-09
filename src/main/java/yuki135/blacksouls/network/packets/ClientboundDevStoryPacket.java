package yuki135.blacksouls.network.packets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundDevStoryPacket(int request,String data) {
    public ClientboundDevStoryPacket(FriendlyByteBuf buffer) {this(buffer.readVarInt(),buffer.readUtf(220000));}
    public void toBytes(FriendlyByteBuf buffer) {buffer.writeVarInt(request);buffer.writeUtf(data,220000);}
    public void handle(Supplier<NetworkEvent.Context> supplier) {PacketHandlers.handleClient(supplier,()->ClientHandler.apply(this));}
    private static final class ClientHandler {
        static void apply(ClientboundDevStoryPacket packet) {if(net.minecraft.client.Minecraft.getInstance().screen instanceof yuki135.blacksouls.client.gui.GuiDevStory screen)screen.receive(packet.request(),packet.data());}
    }
}
