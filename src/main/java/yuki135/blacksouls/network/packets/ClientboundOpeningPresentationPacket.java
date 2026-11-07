package yuki135.blacksouls.network.packets;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import yuki135.blacksouls.story.OriginalOpeningPresentationData;

public record ClientboundOpeningPresentationPacket(UUID session,int revision,UUID episode,long sequence,String program,
        int command,int elapsedFrames,boolean restoring,boolean awaitingDestination,boolean finished,boolean skipped,
        boolean preview,ResourceLocation dimension,BlockPos origin,boolean silentRoom,boolean policyOnly) {
    public ClientboundOpeningPresentationPacket {
        Objects.requireNonNull(session);Objects.requireNonNull(episode);Objects.requireNonNull(program);
        Objects.requireNonNull(dimension);Objects.requireNonNull(origin);
        if(revision<0||sequence<0||program.length()>32||elapsedFrames<0||elapsedFrames>1000000
                ||Math.abs((long)origin.getX())>30000000||Math.abs((long)origin.getZ())>30000000
                ||origin.getY()< -2048||origin.getY()>2048)throw new IllegalArgumentException("Invalid opening cinematic packet");
        OriginalOpeningPresentationData.program(program).cue(command);
        if(policyOnly&&(revision!=0||!episode.equals(new UUID(0,0))||sequence!=0||!program.equals("arrival")||command!=3||elapsedFrames!=30
                ||!restoring||awaitingDestination||!finished||skipped||preview||!origin.equals(BlockPos.ZERO)))throw new IllegalArgumentException("Invalid opening audio policy packet");
        origin=origin.immutable();
    }

    public ClientboundOpeningPresentationPacket(UUID session,int revision,UUID episode,long sequence,String program,int command,int elapsedFrames,
            boolean restoring,boolean awaitingDestination,boolean finished,boolean skipped,boolean preview,ResourceLocation dimension,BlockPos origin,boolean silentRoom) {
        this(session,revision,episode,sequence,program,command,elapsedFrames,restoring,awaitingDestination,finished,skipped,preview,dimension,origin,silentRoom,false);
    }

    public static ClientboundOpeningPresentationPacket policy(UUID owner,boolean silentRoom) {
        return new ClientboundOpeningPresentationPacket(owner,0,new UUID(0,0),0,"arrival",3,30,true,false,true,false,false,
                ResourceLocation.fromNamespaceAndPath("blacksouls","hokoniwa"),BlockPos.ZERO,silentRoom,true);
    }

    public ClientboundOpeningPresentationPacket(FriendlyByteBuf buffer) {
        this(buffer.readUUID(),buffer.readVarInt(),buffer.readUUID(),buffer.readVarLong(),buffer.readUtf(32),
                buffer.readVarInt(),buffer.readVarInt(),buffer.readBoolean(),buffer.readBoolean(),buffer.readBoolean(),buffer.readBoolean(),
                buffer.readBoolean(),buffer.readResourceLocation(),buffer.readBlockPos(),buffer.readBoolean(),buffer.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(session);buffer.writeVarInt(revision);buffer.writeUUID(episode);buffer.writeVarLong(sequence);buffer.writeUtf(program,32);
        buffer.writeVarInt(command);buffer.writeVarInt(elapsedFrames);buffer.writeBoolean(restoring);buffer.writeBoolean(awaitingDestination);
        buffer.writeBoolean(finished);buffer.writeBoolean(skipped);buffer.writeBoolean(preview);buffer.writeResourceLocation(dimension);
        buffer.writeBlockPos(origin);buffer.writeBoolean(silentRoom);buffer.writeBoolean(policyOnly);
    }

    public void handle(Supplier<NetworkEvent.Context> context) { PacketHandlers.handleClient(context,()->ClientHandler.apply(this)); }

    private static final class ClientHandler {
        private static void apply(ClientboundOpeningPresentationPacket packet) { yuki135.blacksouls.client.OriginalOpeningController.applyPresentation(packet); }
    }
}
