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

import java.util.function.Supplier;

public final class ClientboundBonfireEditorPacket {
    private static final int MAX_DIMENSION_ID_LENGTH = 128;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESC_LENGTH = 1024;

    private final BonfireEntry entry;

    public ClientboundBonfireEditorPacket(BonfireEntry entry) {
        this.entry = entry;
    }

    public ClientboundBonfireEditorPacket(FriendlyByteBuf buffer) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(buffer.readUtf(MAX_DIMENSION_ID_LENGTH));
        BlockPos pos = buffer.readBlockPos();
        String name = buffer.readUtf(MAX_NAME_LENGTH);
        String description = buffer.readUtf(MAX_DESC_LENGTH);
        if (dimensionId == null) {
            this.entry = null;
            return;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        this.entry = new BonfireEntry(GlobalPos.of(dimension, pos), name, description);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(entry.pos.dimension().location().toString(), MAX_DIMENSION_ID_LENGTH);
        buffer.writeBlockPos(entry.pos.pos());
        buffer.writeUtf(entry.name, MAX_NAME_LENGTH);
        buffer.writeUtf(entry.description, MAX_DESC_LENGTH);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> {
            if (entry != null) {
                ClientHandler.open(entry);
            }
        });
        return true;
    }

    private static final class ClientHandler {
        private static void open(BonfireEntry entry) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.BlackSouls.BlackSoulsMod.client.gui.GuiBonfireEditor(entry)
            );
        }
    }
}
