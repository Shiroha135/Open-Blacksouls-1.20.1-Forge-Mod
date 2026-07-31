package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public final class ServerboundSetSceneSpawnerBoundsPacket {
    private static final ResourceLocation DEVELOPER_SCEPTER =
            new ResourceLocation("blacksouls", "dev_stat_tool");
    private static final ResourceLocation SCENE_SPAWNER =
            new ResourceLocation("blacksouls2", "scene_spawner");

    private final BlockPos pos;
    private final int rangeX;
    private final int rangeZ;

    public ServerboundSetSceneSpawnerBoundsPacket(BlockPos pos, int rangeX, int rangeZ) {
        this.pos = pos.immutable();
        this.rangeX = rangeX;
        this.rangeZ = rangeZ;
    }

    public ServerboundSetSceneSpawnerBoundsPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        rangeX = buf.readVarInt();
        rangeZ = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(rangeX);
        buf.writeVarInt(rangeZ);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || rangeX < 1 || rangeX > SceneSpawnerBounds.MAX_RANGE
                    || rangeZ < 1 || rangeZ > SceneSpawnerBounds.MAX_RANGE
                    || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D
                    || !holdsDeveloperScepter(player)
                    || !SCENE_SPAWNER.equals(ForgeRegistries.BLOCKS.getKey(player.level().getBlockState(pos).getBlock()))) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (blockEntity instanceof SceneSpawnerBounds bounds) {
                bounds.blacksouls$setBounds(rangeX, rangeZ);
            }
        });
    }

    private static boolean holdsDeveloperScepter(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (DEVELOPER_SCEPTER.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }
}
