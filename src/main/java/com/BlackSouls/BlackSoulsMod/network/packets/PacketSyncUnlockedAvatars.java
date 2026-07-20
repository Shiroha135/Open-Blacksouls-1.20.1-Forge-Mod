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
    private final boolean valid;

    public PacketSyncUnlockedAvatars(ListTag tag) {
        this.avatars = new ArrayList<>();
        for (int i = 0; i < Math.min(tag.size(), MAX_AVATARS); i++) {
            String avatar = tag.getString(i);
            if (avatar.length() <= MAX_AVATAR_ID_LENGTH) {
                this.avatars.add(avatar);
            }
        }
        this.valid = true;
    }

    public PacketSyncUnlockedAvatars(List<String> avatars) {
        this.avatars = new ArrayList<>(Math.min(avatars.size(), MAX_AVATARS));
        for (String avatar : avatars) {
            if (this.avatars.size() >= MAX_AVATARS) {
                break;
            }
            if (avatar != null && avatar.length() <= MAX_AVATAR_ID_LENGTH) {
                this.avatars.add(avatar);
            }
        }
        this.valid = true;
    }

    public PacketSyncUnlockedAvatars(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.avatars = new ArrayList<>();
        if (size < 0 || size > MAX_AVATARS) {
            this.valid = false;
            return;
        }

        for (int i = 0; i < size; i++) {
            this.avatars.add(buf.readUtf(MAX_AVATAR_ID_LENGTH));
        }
        this.valid = true;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.avatars.size());

        for (String avatar : this.avatars) {
            buf.writeUtf(avatar, MAX_AVATAR_ID_LENGTH);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PacketHandlers.handleClient(ctx, () -> {
            if (this.valid) {
                ClientSkillInfo.setUnlockedDlcAvatars(this.avatars);
            }
        });
    }
}
