package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.ClientStoryName;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientboundStoryNamePacket {
    private static final int MAX_NAME_BYTES = 64;
    private final boolean needsChoice;
    private final String storyName;

    public ClientboundStoryNamePacket(boolean needsChoice, String storyName) {
        this.needsChoice = needsChoice;
        this.storyName = storyName;
    }

    public ClientboundStoryNamePacket(FriendlyByteBuf buffer) {
        this.needsChoice = buffer.readBoolean();
        this.storyName = buffer.readUtf(MAX_NAME_BYTES);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(needsChoice);
        buffer.writeUtf(storyName, MAX_NAME_BYTES);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> {
            if (needsChoice) {
                ClientStoryName.requestOpening(storyName);
            } else {
                ClientStoryName.accept(storyName);
            }
        });
    }
}
