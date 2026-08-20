package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundFogGateWalkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
@SuppressWarnings("removal")
public final class FogGateTraversalHandler {
    private static final ResourceLocation FOG_GATE = new ResourceLocation("blacksouls2", "fog_gate");
    private static final double SPEED = 0.115D;
    private static final double TARGET_EPSILON = 0.02D;
    private static final Map<UUID, WalkState> ACTIVE = new HashMap<>();

    public static void start(ServerPlayer player, BlockPos gatePos) {
        if (!player.isAlive()
                || player.isPassenger()
                || !player.level().hasChunkAt(gatePos)
                || player.distanceToSqr(Vec3.atCenterOf(gatePos)) > 64.0D) {
            return;
        }
        BlockState state = player.level().getBlockState(gatePos);
        if (!FOG_GATE.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()))
                || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return;
        }
        finish(player, false);
        Direction direction = traversalDirection(player, gatePos, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        BlockPos targetPos = gatePos.relative(direction);
        double targetX = targetPos.getX() + 0.5D;
        double targetZ = targetPos.getZ() + 0.5D;
        List<BlockPos> gateColumn = findGateColumn(player.level(), gatePos);
        boolean removed = false;
        for (BlockPos pos : gateColumn) {
            removed |= player.level().removeBlock(pos, false);
        }
        if (!removed) {
            return;
        }
        double remaining = remainingDistance(player.getX(), player.getZ(), direction, targetX, targetZ);
        int timeoutTicks = Math.max(80, (int) Math.ceil(Math.max(0.0D, remaining) / SPEED) + 40);
        WalkState walkState = new WalkState(
                direction,
                targetX,
                targetZ,
                player.noPhysics,
                player.isNoGravity(),
                player.tickCount + timeoutTicks
        );
        ACTIVE.put(player.getUUID(), walkState);
        player.noPhysics = true;
        player.setNoGravity(true);
        player.fallDistance = 0.0F;
        face(player, direction);
        NetworkHandler.sendToPlayer(new ClientboundFogGateWalkPacket(direction, targetX, targetZ), player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        WalkState state = ACTIVE.get(player.getUUID());
        if (state == null) {
            return;
        }
        double remaining = remainingDistance(
                player.getX(), player.getZ(), state.direction, state.targetX, state.targetZ
        );
        if (!player.isAlive() || remaining <= TARGET_EPSILON || player.tickCount >= state.timeoutTick) {
            finish(player, true);
            return;
        }
        player.noPhysics = true;
        player.setNoGravity(true);
        player.fallDistance = 0.0F;
        player.setSprinting(false);
        face(player, state.direction);
        double speed = Math.min(SPEED, remaining);
        player.setDeltaMovement(
                state.direction.getStepX() * speed,
                0.0D,
                state.direction.getStepZ() * speed
        );
        player.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            finish(player, false);
        }
    }

    private static Direction traversalDirection(ServerPlayer player, BlockPos gatePos, Direction facing) {
        if (facing.getAxis() == Direction.Axis.X) {
            return player.getX() < gatePos.getX() + 0.5D ? Direction.EAST : Direction.WEST;
        }
        return player.getZ() < gatePos.getZ() + 0.5D ? Direction.SOUTH : Direction.NORTH;
    }

    private static List<BlockPos> findGateColumn(Level level, BlockPos origin) {
        BlockPos bottom = origin;
        while (bottom.getY() > level.getMinBuildHeight() && isFogGate(level.getBlockState(bottom.below()))) {
            bottom = bottom.below();
        }
        List<BlockPos> positions = new ArrayList<>();
        BlockPos cursor = bottom;
        while (cursor.getY() < level.getMaxBuildHeight() && isFogGate(level.getBlockState(cursor))) {
            positions.add(cursor.immutable());
            cursor = cursor.above();
        }
        return positions;
    }

    private static boolean isFogGate(BlockState state) {
        return FOG_GATE.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    private static double remainingDistance(
            double playerX,
            double playerZ,
            Direction direction,
            double targetX,
            double targetZ
    ) {
        return direction.getStepX() * (targetX - playerX)
                + direction.getStepZ() * (targetZ - playerZ);
    }

    private static void face(ServerPlayer player, Direction direction) {
        float yaw = direction.toYRot();
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
    }

    private static void finish(ServerPlayer player, boolean notifyClient) {
        WalkState state = ACTIVE.remove(player.getUUID());
        if (state == null) {
            return;
        }
        player.noPhysics = state.previousNoPhysics;
        player.setNoGravity(state.previousNoGravity);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, movement.y, 0.0D);
        player.hurtMarked = true;
        if (notifyClient) {
            NetworkHandler.sendToPlayer(new ClientboundFogGateWalkPacket(state.direction), player);
        }
    }

    private record WalkState(
            Direction direction,
            double targetX,
            double targetZ,
            boolean previousNoPhysics,
            boolean previousNoGravity,
            int timeoutTick
    ) {
    }

    private FogGateTraversalHandler() {
    }
}
