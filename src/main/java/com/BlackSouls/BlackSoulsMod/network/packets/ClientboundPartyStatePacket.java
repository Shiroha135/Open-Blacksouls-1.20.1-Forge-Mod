package com.BlackSouls.BlackSoulsMod.network.packets;

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
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientHandler.apply(this)
        ));
        context.setPacketHandled(true);
    }

    private static final class ClientHandler {
        private static void apply(ClientboundPartyStatePacket packet) {
            com.BlackSouls.BlackSoulsMod.client.ClientPartyState.setMembers(packet.members);
        }
    }

    public record Member(UUID id, String name, String avatar, float health, float maxHealth,
                         double mp, double maxMp, double ap, double maxAp, int level,
                         boolean leader, boolean downed, List<Effect> effects) {
        private static Member read(FriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            String name = buf.readUtf(64);
            String avatar = buf.readUtf(64);
            float health = buf.readFloat();
            float maxHealth = buf.readFloat();
            double mp = buf.readDouble();
            double maxMp = buf.readDouble();
            double ap = buf.readDouble();
            double maxAp = buf.readDouble();
            int level = buf.readVarInt();
            boolean leader = buf.readBoolean();
            boolean downed = buf.readBoolean();
            int effectCount = Math.min(32, buf.readVarInt());
            List<Effect> effects = new ArrayList<>(effectCount);
            for (int index = 0; index < effectCount; index++) effects.add(Effect.read(buf));
            return new Member(id, name, avatar, health, maxHealth,
                    mp, maxMp, ap, maxAp, level, leader, downed, effects);
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
            buf.writeVarInt(effects.size());
            effects.forEach(effect -> effect.write(buf));
        }
    }

    public record Effect(String id, int duration, int amplifier) {
        private static Effect read(FriendlyByteBuf buf) {
            return new Effect(buf.readUtf(128), buf.readVarInt(), buf.readVarInt());
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(id, 128);
            buf.writeVarInt(duration);
            buf.writeVarInt(amplifier);
        }
    }
}
