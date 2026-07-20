package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTeleportToBonfire {
    private static final int MAX_DIMENSION_ID_LENGTH = 128;
    private static final ResourceLocation LIBRARY_DIMENSION = new ResourceLocation("blacksouls", "library");

    private final ResourceLocation dimension;
    private final BlockPos pos;

    public PacketTeleportToBonfire(GlobalPos globalPos) {
        this.dimension = globalPos.dimension().location();
        this.pos = globalPos.pos();
    }

    public PacketTeleportToBonfire(FriendlyByteBuf buf) {
        this.dimension = ResourceLocation.tryParse(buf.readUtf(MAX_DIMENSION_ID_LENGTH));
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(dimension.toString(), MAX_DIMENSION_ID_LENGTH);
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        PacketHandlers.handleServer(ctx, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || this.dimension == null) {
                return;
            }

            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, this.dimension);
            ServerLevel targetLevel = player.server.getLevel(dimKey);
            boolean isLibrary = LIBRARY_DIMENSION.equals(this.dimension);
            if (targetLevel == null || (!isLibrary && (!targetLevel.isInWorldBounds(this.pos) || !isActivatedBonfire(player, dimKey)))) {
                return;
            }

            if (com.BlackSouls.BlackSoulsMod.BlackSouls.FIRE6_EVENT.isPresent()) {
                player.level().playSound(null, player.blockPosition(), com.BlackSouls.BlackSoulsMod.BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            if (isLibrary) {
                double bonfireX = 5.68;
                double bonfireY = -50.0;
                double bonfireZ = 12.4;
                float targetYaw = 90.0F;

                player.teleportTo(targetLevel, bonfireX, bonfireY, bonfireZ, targetYaw, 0.0F);
            } else {
                BlockPos safePos = this.pos;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos candidate = this.pos.relative(dir);
                    if (targetLevel.getBlockState(candidate).getCollisionShape(targetLevel, candidate).isEmpty() &&
                            targetLevel.getBlockState(candidate.above()).getCollisionShape(targetLevel, candidate.above()).isEmpty()) {
                        safePos = candidate;
                        break;
                    }
                }

                if (safePos.equals(this.pos)) {
                    safePos = this.pos.north();
                }

                double dx = this.pos.getX() - safePos.getX();
                double dz = this.pos.getZ() - safePos.getZ();
                float defaultYaw = (float)(Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;

                player.teleportTo(targetLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, defaultYaw, 15.0F);
            }

            if (com.BlackSouls.BlackSoulsMod.BlackSouls.FIRE6_EVENT.isPresent()) {
                targetLevel.playSound(null, player.blockPosition(), com.BlackSouls.BlackSoulsMod.BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        });
        return true;
    }

    private boolean isActivatedBonfire(ServerPlayer player, ResourceKey<Level> dimensionKey) {
        GlobalPos target = GlobalPos.of(dimensionKey, this.pos);
        BSWorldData data = BSWorldData.get(player.server.overworld());
        return data.activatedBonfires.stream().anyMatch(entry -> entry.pos.equals(target));
    }
}
