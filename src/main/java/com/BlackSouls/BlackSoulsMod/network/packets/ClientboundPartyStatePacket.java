package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.ClientPartyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class ClientboundPartyStatePacket {
    private final List<Member> members;

    public ClientboundPartyStatePacket(List<Member> members) { this.members = List.copyOf(members); }
    public ClientboundPartyStatePacket(FriendlyByteBuf buf) {
        int count = Math.min(4, buf.readVarInt());
        List<Member> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) decoded.add(Member.read(buf));
        this.members = decoded;
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.members.size());
        this.members.forEach(member -> member.write(buf));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPartyState.setMembers(this.members)));
        context.setPacketHandled(true);
    }

    public record Member(UUID id, String name, String avatar, float health, float maxHealth,
                         double mp, double maxMp, double ap, double maxAp, int level,
                         boolean leader, boolean downed) {
        private static Member read(FriendlyByteBuf buf) {
            return new Member(buf.readUUID(), buf.readUtf(64), buf.readUtf(64), buf.readFloat(), buf.readFloat(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
        }
        private void write(FriendlyByteBuf buf) {
            buf.writeUUID(id);
            buf.writeUtf(name, 64);
            buf.writeUtf(avatar, 64);
            buf.writeFloat(health);
            buf.writeFloat(maxHealth);
            buf.writeDouble(mp);
            buf.writeDouble(maxMp);
            buf.writeDouble(ap);
            buf.writeDouble(maxAp);
            buf.writeVarInt(level);
            buf.writeBoolean(leader);
            buf.writeBoolean(downed);
        }
    }
}
