package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BonfireEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncBonfireList {
    private static final int MAX_BONFIRES = 256;
    private static final int MAX_NAME_LENGTH = 1024;
    private static final int MAX_DESC_LENGTH = 2048;
    private static final int MAX_DIMENSION_ID_LENGTH = 128;

    private final List<BonfireEntry> bonfires;
    private final boolean valid;

    public PacketSyncBonfireList(List<BonfireEntry> originalList) {
        this.bonfires = new ArrayList<>(Math.min(originalList.size(), MAX_BONFIRES));
        for (BonfireEntry entry : originalList) {
            if (this.bonfires.size() >= MAX_BONFIRES) {
                break;
            }
            if (entry == null || entry.pos == null || entry.name == null) {
                continue;
            }
            this.bonfires.add(entry);
        }
        this.valid = true;
    }

    public PacketSyncBonfireList(FriendlyByteBuf buf) {
        this.bonfires = new ArrayList<>();
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_BONFIRES) {
            this.valid = false;
            return;
        }
        for (int i = 0; i < size; i++) {
            ResourceLocation dimLoc = ResourceLocation.tryParse(buf.readUtf(MAX_DIMENSION_ID_LENGTH));
            BlockPos pos = buf.readBlockPos();
            String name = buf.readUtf(MAX_NAME_LENGTH);
            String desc = buf.readUtf(MAX_DESC_LENGTH);
            if (dimLoc != null) {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimLoc);
                bonfires.add(new BonfireEntry(GlobalPos.of(dim, pos), name, desc));
            }
        }
        this.valid = true;
    }

    public void toBytes(FriendlyByteBuf buf) {
        int size = Math.min(bonfires.size(), MAX_BONFIRES);
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            BonfireEntry entry = bonfires.get(i);
            buf.writeUtf(entry.pos.dimension().location().toString(), MAX_DIMENSION_ID_LENGTH);
            buf.writeBlockPos(entry.pos.pos());
            buf.writeUtf(entry.name, MAX_NAME_LENGTH);
            buf.writeUtf(entry.description != null ? entry.description : "", MAX_DESC_LENGTH);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        PacketHandlers.handleClient(ctx, () -> {
            if (this.valid) {
                ClientHandler.handle(this);
            }
        });
        return true;
    }

    private static class ClientHandler {
        public static void handle(PacketSyncBonfireList msg) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.BlackSouls.BlackSoulsMod.client.gui.GuiBonfireRest(msg.bonfires));
        }
    }
}
