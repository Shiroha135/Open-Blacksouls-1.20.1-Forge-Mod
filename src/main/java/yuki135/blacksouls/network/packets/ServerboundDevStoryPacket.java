package yuki135.blacksouls.network.packets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import yuki135.blacksouls.story.DeveloperStoryService;

public record ServerboundDevStoryPacket(int request,String action,String key,String value,String token) {
    public ServerboundDevStoryPacket(FriendlyByteBuf buffer) {this(buffer.readVarInt(),buffer.readUtf(32),buffer.readUtf(128),buffer.readUtf(128),buffer.readUtf(64));}
    public void toBytes(FriendlyByteBuf buffer) {buffer.writeVarInt(request);buffer.writeUtf(action,32);buffer.writeUtf(key,128);buffer.writeUtf(value,128);buffer.writeUtf(token,64);}
    public void handle(Supplier<NetworkEvent.Context> supplier) {PacketHandlers.handleOpeningServer(supplier,context->DeveloperStoryService.handle(context.getSender(),this));}
}
