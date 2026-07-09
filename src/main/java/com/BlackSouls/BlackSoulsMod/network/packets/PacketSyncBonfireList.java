package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BonfireEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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

    public PacketSyncBonfireList(List<BonfireEntry> originalList) {
        
        this.bonfires = new ArrayList<>(originalList);

        this.bonfires.removeIf(entry ->
                entry.pos.dimension().location().getPath().contains("library") ||
                        entry.name.contains("图书馆") ||
                        entry.name.contains("library_name")
        );

        
        ResourceKey<Level> libKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("blacksouls", "library"));
        GlobalPos libPos = GlobalPos.of(libKey, BlockPos.ZERO);

        String transName = Component.translatable("gui.blacksouls.bonfire.library_name").getString();
        String transDesc = Component.translatable("gui.blacksouls.bonfire.library_desc").getString();

        this.bonfires.add(0, new BonfireEntry(libPos, transName, transDesc));
    }

    public PacketSyncBonfireList(FriendlyByteBuf buf) {
        this.bonfires = new ArrayList<>();
        int size = Math.min(Math.max(0, buf.readVarInt()), MAX_BONFIRES);
        for (int i = 0; i < size; i++) {
            ResourceLocation dimLoc = ResourceLocation.tryParse(buf.readUtf(MAX_DIMENSION_ID_LENGTH));
            if (dimLoc == null) {
                dimLoc = Level.OVERWORLD.location();
            }
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimLoc);
            BlockPos pos = buf.readBlockPos();
            String name = buf.readUtf(MAX_NAME_LENGTH);
            String desc = buf.readUtf(MAX_DESC_LENGTH);
            bonfires.add(new BonfireEntry(GlobalPos.of(dim, pos), name, desc));
        }
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
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        ctx.get().setPacketHandled(true);
        return true;
    }

    private static class ClientHandler {
        public static void handle(PacketSyncBonfireList msg) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.BlackSouls.BlackSoulsMod.client.gui.GuiBonfireRest(msg.bonfires));
        }
    }
}
