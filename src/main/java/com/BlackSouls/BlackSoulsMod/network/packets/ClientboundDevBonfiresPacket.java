package com.BlackSouls.BlackSoulsMod.network.packets;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ClientboundDevBonfiresPacket {
    public static final int MAX_ENTRIES = 1024;
    private final List<Entry> entries;
    private final boolean successful;

    public record Entry(GlobalPos pos, String name, String description, boolean activated) {
    }

    public ClientboundDevBonfiresPacket(List<Entry> entries, boolean successful) {
        this.entries = entries.stream().limit(MAX_ENTRIES).toList();
        this.successful = successful;
    }

    public ClientboundDevBonfiresPacket(FriendlyByteBuf buffer) {
        this.successful = buffer.readBoolean();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid developer bonfire count: " + count);
        }
        this.entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buffer.readGlobalPos(), buffer.readUtf(512), buffer.readUtf(1024), buffer.readBoolean()));
        }
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.successful);
        buffer.writeVarInt(this.entries.size());
        for (Entry entry : this.entries) {
            buffer.writeGlobalPos(entry.pos());
            buffer.writeUtf(entry.name().substring(0, Math.min(512, entry.name().length())), 512);
            buffer.writeUtf(entry.description().substring(0, Math.min(1024, entry.description().length())), 1024);
            buffer.writeBoolean(entry.activated());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> ClientHandler.receive(this));
    }

    private static final class ClientHandler {
        private static void receive(ClientboundDevBonfiresPacket packet) {
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof com.BlackSouls.BlackSoulsMod.client.gui.GuiDevPanel panel) {
                panel.receiveBonfires(packet.entries, packet.successful);
            }
        }
    }
}
