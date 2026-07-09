package com.BlackSouls.BlackSoulsMod.network.packets;

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
    private ResourceLocation dimension;
    private BlockPos pos;

    public PacketTeleportToBonfire(GlobalPos globalPos) {
        this.dimension = globalPos.dimension().location();
        this.pos = globalPos.pos();
    }

    public PacketTeleportToBonfire(FriendlyByteBuf buf) {
        this.dimension = new ResourceLocation(buf.readUtf());
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(dimension.toString());
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimension);
                ServerLevel targetLevel = player.server.getLevel(dimKey);

                if (targetLevel != null) {
                    if (com.BlackSouls.BlackSoulsMod.BlackSouls.FIRE6_EVENT.isPresent()) {
                        player.level().playSound(null, player.blockPosition(), com.BlackSouls.BlackSoulsMod.BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                    if (dimension.getNamespace().equals("blacksouls") && dimension.getPath().equals("library")) {

                        double bonfireX = 5.68;
                        double bonfireY = -50.0;
                        double bonfireZ = 12.4;
                        float targetYaw = 90.0F;

                        player.teleportTo(targetLevel, bonfireX, bonfireY, bonfireZ, targetYaw, 0.0F);
                    } else {
                        BlockPos safePos = pos;
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            BlockPos candidate = pos.relative(dir);
                            if (targetLevel.getBlockState(candidate).getCollisionShape(targetLevel, candidate).isEmpty() &&
                                    targetLevel.getBlockState(candidate.above()).getCollisionShape(targetLevel, candidate.above()).isEmpty()) {
                                safePos = candidate;
                                break;
                            }
                        }

                        if (safePos.equals(pos)) {
                            safePos = pos.north();
                        }

                        double dx = pos.getX() - safePos.getX();
                        double dz = pos.getZ() - safePos.getZ();
                        float defaultYaw = (float)(Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;

                        player.teleportTo(targetLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, defaultYaw, 15.0F);
                    }

                    if (com.BlackSouls.BlackSoulsMod.BlackSouls.FIRE6_EVENT.isPresent()) {
                        targetLevel.playSound(null, player.blockPosition(), com.BlackSouls.BlackSoulsMod.BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}