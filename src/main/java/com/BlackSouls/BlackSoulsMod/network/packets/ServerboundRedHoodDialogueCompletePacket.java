package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.entity.EntityRedHood;
import com.BlackSouls.BlackSoulsMod.handler.RedHoodStoryHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundRedHoodDialogueCompletePacket {
    private final int entityId;
    private final int storyStage;

    public ServerboundRedHoodDialogueCompletePacket(int entityId, int storyStage) {
        this.entityId = entityId;
        this.storyStage = storyStage;
    }

    public ServerboundRedHoodDialogueCompletePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.storyStage = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.storyStage);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || this.storyStage < 0) {
                return;
            }
            Entity target = player.level().getEntity(this.entityId);
            if (target instanceof EntityRedHood redHood && player.distanceToSqr(redHood) <= 64.0D) {
                RedHoodStoryHandler.completeDialogue(player, redHood, this.storyStage);
            }
        });
        context.setPacketHandled(true);
    }
}
