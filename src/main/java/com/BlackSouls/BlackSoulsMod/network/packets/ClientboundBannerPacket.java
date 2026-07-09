package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.render.SeekServiceBannerRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.SoulGainBannerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundBannerPacket {
    private final Type type;
    private final long delta;

    public ClientboundBannerPacket(Type type, long delta) {
        this.type = type;
        this.delta = delta;
    }

    public ClientboundBannerPacket(FriendlyByteBuf buf) {
        this.type = PacketHandlers.readEnum(buf, Type.values());
        this.delta = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf) {
        PacketHandlers.writeEnum(buf, this.type);
        buf.writeLong(this.delta);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> ClientHandler.handle(this.type, this.delta));
    }

    public enum Type {
        SEEK_SERVICE,
        SOUL_GAIN
    }

    private static class ClientHandler {
        private static void handle(Type type, long delta) {
            switch (type) {
                case SEEK_SERVICE -> SeekServiceBannerRenderer.show(clampToInt(delta));
                case SOUL_GAIN -> SoulGainBannerRenderer.show(delta);
            }
        }

        private static int clampToInt(long value) {
            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (value < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return (int) value;
        }
    }
}
