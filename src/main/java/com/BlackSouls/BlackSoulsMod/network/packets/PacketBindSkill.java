package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketBindSkill {
    private String keyBind; 
    private String skillName;

    public PacketBindSkill() {}

    public PacketBindSkill(String keyBind, String skillName) {
        this.keyBind = keyBind;
        this.skillName = skillName;
    }

    public PacketBindSkill(FriendlyByteBuf buf) {
        this.keyBind = buf.readUtf();
        this.skillName = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(keyBind);
        buf.writeUtf(skillName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
                if (stats != null) {
                    if (keyBind.equals("Z")) stats.skillZ = skillName;
                    else if (keyBind.equals("X")) stats.skillX = skillName;
                    else if (keyBind.equals("C")) stats.skillC = skillName;
                    else if (keyBind.equals("V")) stats.skillV = skillName;

                    StatEventHandler.syncToClient(player);
                }
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}