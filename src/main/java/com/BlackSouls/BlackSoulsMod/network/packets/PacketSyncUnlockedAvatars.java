package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.ClientSkillInfo;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncUnlockedAvatars {
    private static final int MAX_AVATARS = 512;
    private static final int MAX_AVATAR_ID_LENGTH = 128;

    private final List<String> avatars;

    public PacketSyncUnlockedAvatars(ListTag tag) {
        this.avatars = new ArrayList<>();
        for (int i = 0; i < Math.min(tag.size(), MAX_AVATARS); i++) {
            this.avatars.add(tag.getString(i));
        }
    }

    public PacketSyncUnlockedAvatars(List<String> avatars) {
        this.avatars = new ArrayList<>(avatars.subList(0, Math.min(avatars.size(), MAX_AVATARS)));
    }

    public PacketSyncUnlockedAvatars(FriendlyByteBuf buf) {
        int size = Math.min(Math.max(0, buf.readVarInt()), MAX_AVATARS);
        this.avatars = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            this.avatars.add(buf.readUtf(MAX_AVATAR_ID_LENGTH));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.avatars.size());

        for (String avatar : this.avatars) {
            buf.writeUtf(avatar, MAX_AVATAR_ID_LENGTH);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientSkillInfo.setUnlockedDlcAvatars(this.avatars));
        ctx.get().setPacketHandled(true);
    }
}
