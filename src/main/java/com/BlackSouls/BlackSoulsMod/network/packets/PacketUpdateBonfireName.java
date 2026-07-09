package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import com.BlackSouls.BlackSoulsMod.capability.BonfireEntry;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketUpdateBonfireName {
    private final GlobalPos pos;
    private final String newName;
    private final String newDesc;

    public PacketUpdateBonfireName(GlobalPos pos, String newName, String newDesc) {
        this.pos = pos;
        this.newName = newName;
        this.newDesc = newDesc;
    }

    public PacketUpdateBonfireName(FriendlyByteBuf buf) {
        this.pos = buf.readGlobalPos();
        this.newName = buf.readUtf();
        this.newDesc = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeGlobalPos(pos);
        buf.writeUtf(newName);
        buf.writeUtf(newDesc);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.getServer() != null) {
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
        ctx.get().setPacketHandled(true);
        return true;
    }
}