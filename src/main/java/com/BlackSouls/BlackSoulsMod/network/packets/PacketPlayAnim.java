package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.render.ClientVFXHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketPlayAnim {
    private final int animId;
    private final double x, y, z;

    public PacketPlayAnim(int animId, double x, double y, double z) {
        this.animId = animId;
        this.x = x; this.y = y; this.z = z;
    }

    public PacketPlayAnim(FriendlyByteBuf buf) {
        this.animId = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.animId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        ctx.get().setPacketHandled(true);
        return true;
    }

    private static class ClientHandler {
        public static void handle(PacketPlayAnim msg) {
            ClientVFXHandler.playAnim(msg.animId, msg.x, msg.y, msg.z);
        }
    }
}
