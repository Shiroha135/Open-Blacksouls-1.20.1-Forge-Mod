package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.AnimatedDoorSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AnimatedDoorHandler {
    private static final int PHASE_TICKS = 10;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getEntity().getMainHandItem().is(BlackSouls.DEV_STAT_TOOL.get())) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!isSupportedDoor(state)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = normalizeDoorPos(level, event.getPos(), state);
        boolean enabled = AnimatedDoorSavedData.get(level).toggle(pos);
        if (enabled) {
            boolean open = currentPhase(level);
            applyState(level, pos, open);
        }
        event.getEntity().displayClientMessage(
                Component.translatable(enabled
                        ? "message.blacksouls.dev.animated_door.enabled"
                        : "message.blacksouls.dev.animated_door.disabled"),
                true
        );
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % PHASE_TICKS != 0) {
            return;
        }
        boolean open = currentPhase(event.getServer().overworld());
        for (ServerLevel level : event.getServer().getAllLevels()) {
            AnimatedDoorSavedData data = AnimatedDoorSavedData.get(level);
            for (long packedPos : data.positions()) {
                BlockPos pos = BlockPos.of(packedPos);
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (!isSupportedDoor(state)) {
                    data.remove(packedPos);
                    continue;
                }
                applyState(level, pos, open);
            }
        }
    }

    private static boolean currentPhase(ServerLevel level) {
        return (level.getServer().getTickCount() / PHASE_TICKS & 1) == 0;
    }

    private static void applyState(ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.OPEN)
                || state.getValue(BlockStateProperties.OPEN) == open) {
            return;
        }
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(null, level, state, pos, open);
            return;
        }
        level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), 10);
        level.playSound(
                null,
                pos,
                state.is(BlockTags.WOODEN_TRAPDOORS) || state.getBlock() instanceof FenceGateBlock
                        ? open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE
                        : open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
        level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    private static BlockPos normalizeDoorPos(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock
                && state.hasProperty(DoorBlock.HALF)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos.immutable();
    }

    private static boolean isSupportedDoor(BlockState state) {
        return state.hasProperty(BlockStateProperties.OPEN)
                && (state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock);
    }

    private AnimatedDoorHandler() {
    }
}
