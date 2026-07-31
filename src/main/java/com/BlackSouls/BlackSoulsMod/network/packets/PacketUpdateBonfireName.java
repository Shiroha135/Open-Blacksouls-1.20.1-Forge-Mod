package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import com.BlackSouls.BlackSoulsMod.util.BonfireMetadata;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketUpdateBonfireName {
    private static final int MAX_DIMENSION_ID_LENGTH = 128;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESC_LENGTH = 1024;

    private final GlobalPos pos;
    private final String newName;
    private final String newDesc;

    public PacketUpdateBonfireName(GlobalPos pos, String newName, String newDesc) {
        this.pos = pos;
        this.newName = newName;
        this.newDesc = newDesc;
    }

    public PacketUpdateBonfireName(FriendlyByteBuf buf) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(buf.readUtf(MAX_DIMENSION_ID_LENGTH));
        net.minecraft.core.BlockPos blockPos = buf.readBlockPos();
        this.pos = dimensionId == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimensionId), blockPos);
        this.newName = buf.readUtf(MAX_NAME_LENGTH);
        this.newDesc = buf.readUtf(MAX_DESC_LENGTH);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(pos.dimension().location().toString(), MAX_DIMENSION_ID_LENGTH);
        buf.writeBlockPos(pos.pos());
        buf.writeUtf(newName, MAX_NAME_LENGTH);
        buf.writeUtf(newDesc, MAX_DESC_LENGTH);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        PacketHandlers.handleServer(ctx, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null || this.pos == null
                    || !player.isCreative()
                    || !player.level().dimension().equals(this.pos.dimension())
                    || player.blockPosition().distSqr(this.pos.pos()) > 64.0D
                    || !player.getMainHandItem().is(BlackSouls.DEV_STAT_TOOL.get())) {
                return;
            }
            ServerLevel level = player.getServer().getLevel(this.pos.dimension());
            if (level == null || !level.getBlockState(this.pos.pos()).is(BlockTags.CAMPFIRES)) {
                return;
            }
            String cleanName = this.newName.strip();
            if (cleanName.isBlank()) {
                cleanName = BonfireMetadata.DEFAULT_NAME;
            }
            String cleanDescription = this.newDesc.strip();
            if (BonfireMetadata.write(level, this.pos.pos(), cleanName, cleanDescription)) {
                BSWorldData data = BSWorldData.get(player.getServer().overworld());
                data.updateBonfire(this.pos, cleanName, cleanDescription);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.blacksouls.bonfire.editor.saved"),
                        true
                );
            }
        });
        return true;
    }
}
