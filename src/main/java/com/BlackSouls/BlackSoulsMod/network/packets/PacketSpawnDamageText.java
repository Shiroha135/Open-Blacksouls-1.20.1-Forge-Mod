package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.render.DamageTextRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSpawnDamageText {
    private final double x, y, z;
    private final float damage;
    private final boolean isCrit;

    public PacketSpawnDamageText(double x, double y, double z, float damage, boolean isCrit) {
        this.x = x; this.y = y; this.z = z;
        this.damage = damage; this.isCrit = isCrit;
    }

    public PacketSpawnDamageText(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.damage = buf.readFloat();
        this.isCrit = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(this.x); buf.writeDouble(this.y); buf.writeDouble(this.z);
        buf.writeFloat(this.damage); buf.writeBoolean(this.isCrit);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        ctx.get().setPacketHandled(true);
    }

    private static class ClientHandler {
        public static void handle(PacketSpawnDamageText msg) {
            DamageTextRenderer.addText(msg.x, msg.y, msg.z, msg.damage, msg.isCrit);
        }
    }
}