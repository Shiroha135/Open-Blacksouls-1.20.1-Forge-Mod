package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncMana {
    private final float mana;

    public PacketSyncMana(float mana) { this.mana = mana; }
    public PacketSyncMana(FriendlyByteBuf buf) { this.mana = buf.readFloat(); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeFloat(this.mana); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        ctx.get().setPacketHandled(true);
    }

    private static class ClientHandler {
        public static void handle(PacketSyncMana msg) {
            if (net.minecraft.client.Minecraft.getInstance().player != null) {
                SkillUtils.setMana(net.minecraft.client.Minecraft.getInstance().player, msg.mana);
            }
        }
    }
}
