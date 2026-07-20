package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import com.BlackSouls.BlackSoulsMod.capability.BonfireEntry;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketUpdateBonfireName {
    private static final int MAX_DIMENSION_ID_LENGTH = 128;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESC_LENGTH = 1024;

    private final GlobalPos pos;
    private final String newName;
    private final String newDesc;

    public PacketUpdateBonfireName(GlobalPos pos, String newName, String newDesc) {
        this.pos = pos;
        this.newName = newName;
        this.newDesc = newDesc;
    }

    public PacketUpdateBonfireName(FriendlyByteBuf buf) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(buf.readUtf(MAX_DIMENSION_ID_LENGTH));
        net.minecraft.core.BlockPos blockPos = buf.readBlockPos();
        this.pos = dimensionId == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimensionId), blockPos);
        this.newName = buf.readUtf(MAX_NAME_LENGTH);
        this.newDesc = buf.readUtf(MAX_DESC_LENGTH);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(pos.dimension().location().toString(), MAX_DIMENSION_ID_LENGTH);
        buf.writeBlockPos(pos.pos());
        buf.writeUtf(newName, MAX_NAME_LENGTH);
        buf.writeUtf(newDesc, MAX_DESC_LENGTH);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        PacketHandlers.handleServer(ctx, context -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getServer() != null && this.pos != null && !this.newName.isBlank()) {
                BSWorldData data = BSWorldData.get(player.getServer().overworld());
                for (BonfireEntry entry : data.activatedBonfires) {
                    if (entry.pos.equals(pos)) {
                        entry.name = newName;
                        entry.description = newDesc;
                        data.setDirty();
                        break;
                    }
                }
            }
        });
        return true;
    }
}
