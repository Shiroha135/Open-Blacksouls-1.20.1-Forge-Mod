package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.AnimatedDoorSavedData;
import com.BlackSouls.BlackSoulsMod.capability.DoorEventSavedData;
import com.BlackSouls.BlackSoulsMod.capability.DoorEventProgressSavedData;
import com.BlackSouls.BlackSoulsMod.capability.DoorEventSavedData.DoorEvent;
import com.BlackSouls.BlackSoulsMod.capability.DoorEventSavedData.EventRole;
import com.BlackSouls.BlackSoulsMod.capability.DoorLockSavedData;
import com.BlackSouls.BlackSoulsMod.capability.DoorLockSavedData.DoorLock;
import com.BlackSouls.BlackSoulsMod.capability.DoorLockSavedData.LockType;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundDoorEditorPacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundSaveDoorConfigPacket;
import com.BlackSouls.BlackSoulsMod.util.DoorConfigMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.registries.ForgeRegistries;

public final class DoorEditorService {
    public static void open(ServerPlayer player, ServerLevel level, BlockPos clickedPos) {
        if (!player.isCreative()) {
            player.displayClientMessage(Component.translatable("message.blacksouls.dev.no_permission"), true);
            return;
        }
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!isSupported(clickedState)) {
            return;
        }
        BlockPos pos = normalize(level, clickedPos, clickedState);
        DoorConfigMode mode = DoorConfigMode.NONE;
        String requiredItem = "minecraft:air";
        boolean consume = true;
        String eventId = "Map051 EV008";
        String conditionId = "Map051 EV033";
        String targetDimension = level.dimension().location().toString();
        double targetX = player.getX();
        double targetY = player.getY();
        double targetZ = player.getZ();
        boolean eventTriggered = false;

        DoorEvent doorEvent = DoorEventSavedData.get(level).getEvent(pos);
        if (doorEvent != null) {
            mode = doorEvent.role() == EventRole.SHORTCUT_GATE
                    ? DoorConfigMode.SHORTCUT_GATE
                    : DoorConfigMode.SHORTCUT_UNLOCK;
            eventId = doorEvent.eventId();
            conditionId = doorEvent.conditionId();
            targetDimension = doorEvent.targetDimension().toString();
            targetX = doorEvent.targetX();
            targetY = doorEvent.targetY();
            targetZ = doorEvent.targetZ();
            eventTriggered = DoorEventProgressSavedData.get(level.getServer()).isTriggered(conditionId);
        } else if (AnimatedDoorSavedData.get(level).contains(pos)) {
            mode = DoorConfigMode.ANIMATED_GROUP;
        } else {
            DoorLock lock = DoorLockSavedData.get(level).getLock(pos);
            if (lock != null) {
                mode = lock.type() == LockType.NORMAL
                        ? DoorConfigMode.NORMAL_LOCK
                        : DoorConfigMode.STORY_LOCK;
                consume = lock.consume();
                if (lock.requiredItem() != null) {
                    requiredItem = lock.requiredItem().toString();
                }
            }
        }

        NetworkHandler.sendToPlayer(new ClientboundDoorEditorPacket(
                pos,
                mode,
                requiredItem,
                consume,
                eventId,
                conditionId,
                targetDimension,
                targetX,
                targetY,
                targetZ,
                eventTriggered
        ), player);
    }

    public static void apply(ServerPlayer player, ServerboundSaveDoorConfigPacket packet) {
        if (!player.isCreative()
                || !player.getMainHandItem().is(BlackSouls.DEV_STAT_TOOL.get())
                || player.distanceToSqr(
                        packet.pos().getX() + 0.5D,
                        packet.pos().getY() + 0.5D,
                        packet.pos().getZ() + 0.5D
                ) > 64.0D
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = level.getBlockState(packet.pos());
        if (!isSupported(state) || !normalize(level, packet.pos(), state).equals(packet.pos())) {
            return;
        }

        ResourceLocation requiredItem = null;
        if (packet.mode() == DoorConfigMode.STORY_LOCK) {
            requiredItem = ResourceLocation.tryParse(packet.requiredItem().strip());
            if (requiredItem == null
                    || requiredItem.getNamespace().equals("minecraft") && requiredItem.getPath().equals("air")
                    || !ForgeRegistries.ITEMS.containsKey(requiredItem)) {
                return;
            }
        }

        ResourceLocation targetDimension = null;
        if (packet.mode() == DoorConfigMode.SHORTCUT_GATE
                || packet.mode() == DoorConfigMode.SHORTCUT_UNLOCK) {
            targetDimension = ResourceLocation.tryParse(packet.targetDimension().strip());
            if (targetDimension == null
                    || packet.eventId().strip().isEmpty()
                    || packet.conditionId().strip().isEmpty()
                    || !validCoordinates(packet.targetX(), packet.targetY(), packet.targetZ())
                    || player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, targetDimension)) == null) {
                return;
            }
        }

        DoorLockSavedData.get(level).removeLock(packet.pos());
        AnimatedDoorSavedData.get(level).set(packet.pos(), false);
        DoorEventSavedData.get(level).removeEvent(packet.pos());

        switch (packet.mode()) {
            case NORMAL_LOCK -> DoorLockSavedData.get(level).setLock(
                    packet.pos(),
                    new DoorLock(LockType.NORMAL, null, packet.consume())
            );
            case STORY_LOCK -> DoorLockSavedData.get(level).setLock(
                    packet.pos(),
                    new DoorLock(LockType.STORY, requiredItem, packet.consume())
            );
            case ANIMATED_GROUP -> AnimatedDoorSavedData.get(level).set(packet.pos(), true);
            case SHORTCUT_GATE, SHORTCUT_UNLOCK -> {
                String eventId = packet.eventId().strip();
                String conditionId = packet.conditionId().strip();
                DoorEventSavedData.get(level).setEvent(packet.pos(), new DoorEvent(
                        packet.mode() == DoorConfigMode.SHORTCUT_GATE
                                ? EventRole.SHORTCUT_GATE
                                : EventRole.SHORTCUT_UNLOCK,
                        eventId,
                        conditionId,
                        targetDimension,
                        packet.targetX(),
                        packet.targetY(),
                        packet.targetZ()
                ));
                DoorEventProgressSavedData.get(level.getServer()).setTriggered(
                        conditionId,
                        packet.eventTriggered()
                );
            }
            case NONE -> {
            }
        }
        if (packet.mode() != DoorConfigMode.NONE && packet.mode() != DoorConfigMode.ANIMATED_GROUP) {
            close(level, packet.pos(), player);
        }
        player.displayClientMessage(Component.translatable("message.blacksouls.door.editor.saved"), true);
    }

    public static boolean isSupported(BlockState state) {
        return state.hasProperty(BlockStateProperties.OPEN)
                && (state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock);
    }

    public static BlockPos normalize(LevelReader level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock
                && state.hasProperty(DoorBlock.HALF)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos.immutable();
    }

    public static void close(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.OPEN) || !state.getValue(BlockStateProperties.OPEN)) {
            return;
        }
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(player, level, state, pos, false);
        } else {
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, false), 10);
        }
    }

    private static boolean validCoordinates(double x, double y, double z) {
        return Double.isFinite(x)
                && Double.isFinite(y)
                && Double.isFinite(z)
                && Math.abs(x) <= 30_000_000.0D
                && Math.abs(y) <= 20_000_000.0D
                && Math.abs(z) <= 30_000_000.0D;
    }

    private DoorEditorService() {
    }
}
