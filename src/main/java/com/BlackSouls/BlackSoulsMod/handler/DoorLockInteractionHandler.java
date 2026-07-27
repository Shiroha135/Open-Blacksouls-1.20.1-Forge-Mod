package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.DoorLockSavedData;
import com.BlackSouls.BlackSoulsMod.capability.DoorLockSavedData.DoorLock;
import com.BlackSouls.BlackSoulsMod.capability.DoorLockSavedData.LockType;
import com.BlackSouls.BlackSoulsMod.event.LockedDoorInteractEvent;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundTextBannerPacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundLostItemPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DoorLockInteractionHandler {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockState clickedState = event.getLevel().getBlockState(event.getPos());
        if (!isLockable(clickedState)) {
            return;
        }
        BlockPos lockPos = normalizeDoorPos(event.getLevel() instanceof ServerLevel serverLevel
                ? serverLevel
                : null, event.getPos(), clickedState);

        if (event.getEntity().isShiftKeyDown()
                && event.getEntity().getMainHandItem().is(BlackSouls.DEV_STAT_TOOL.get())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
                configureLock(player, level, lockPos);
            }
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        DoorLockSavedData data = DoorLockSavedData.get(level);
        DoorLock lock = data.getLock(lockPos);
        if (lock == null) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        InteractionHand keyHand = findKeyHand(player, lock);
        if (keyHand == null) {
            NetworkHandler.sendToPlayer(
                    new ClientboundTextBannerPacket(Component.translatable("message.blacksouls.lock.locked")),
                    player
            );
            level.playSound(null, lockPos, BlackSouls.LOCK_RATTLE_EVENT.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            BlockState authoritativeState = level.getBlockState(lockPos);
            level.sendBlockUpdated(lockPos, authoritativeState, authoritativeState, 3);
            if (authoritativeState.getBlock() instanceof DoorBlock) {
                BlockState upperState = level.getBlockState(lockPos.above());
                level.sendBlockUpdated(lockPos.above(), upperState, upperState, 3);
            }
            MinecraftForge.EVENT_BUS.post(new LockedDoorInteractEvent(player, level, lockPos, lock));
            return;
        }

        ItemStack key = player.getItemInHand(keyHand);
        if (lock.consume() && !player.getAbilities().instabuild) {
            ItemStack lost = key.copy();
            lost.setCount(1);
            key.shrink(1);
            NetworkHandler.sendToPlayer(new ClientboundLostItemPacket(lost), player);
        }
        data.removeLock(lockPos);
        open(level, lockPos, player);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        if (!isLockable(state)) {
            return;
        }
        DoorLockSavedData.get(level).removeLock(normalizeDoorPos(level, event.getPos(), state));
    }

    private static void configureLock(ServerPlayer player, ServerLevel level, BlockPos pos) {
        DoorLockSavedData data = DoorLockSavedData.get(level);
        ItemStack requiredStack = player.getOffhandItem();
        ResourceLocation requiredItem = requiredStack.isEmpty()
                ? null
                : BuiltInRegistries.ITEM.getKey(requiredStack.getItem());
        DoorLock desired = requiredItem == null
                ? new DoorLock(LockType.NORMAL, null, true)
                : new DoorLock(LockType.STORY, requiredItem, true);
        DoorLock existing = data.getLock(pos);
        if (desired.equals(existing)) {
            data.removeLock(pos);
            player.displayClientMessage(Component.translatable("message.blacksouls.lock.dev.removed"), true);
            return;
        }
        data.setLock(pos, desired);
        close(level, pos, player);
        if (desired.type() == LockType.NORMAL) {
            player.displayClientMessage(Component.translatable("message.blacksouls.lock.dev.normal"), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.blacksouls.lock.dev.story", requiredStack.getHoverName()),
                    true
            );
        }
    }

    private static InteractionHand findKeyHand(ServerPlayer player, DoorLock lock) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (lock.type() == LockType.NORMAL && stack.is(BlackSouls.MASTER_KEY.get())) {
                return hand;
            }
            if (lock.type() == LockType.STORY
                    && lock.requiredItem() != null
                    && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(lock.requiredItem())) {
                return hand;
            }
        }
        return null;
    }

    private static BlockPos normalizeDoorPos(ServerLevel level, BlockPos pos, BlockState state) {
        if (level != null
                && state.getBlock() instanceof DoorBlock
                && state.hasProperty(DoorBlock.HALF)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos.immutable();
    }

    private static boolean isLockable(BlockState state) {
        return state.hasProperty(BlockStateProperties.OPEN)
                && (state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock);
    }

    private static void open(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(player, level, state, pos, true);
            return;
        }
        if (state.hasProperty(BlockStateProperties.OPEN)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, true), 10);
            level.playSound(
                    null,
                    pos,
                    state.is(BlockTags.WOODEN_TRAPDOORS) || state.getBlock() instanceof FenceGateBlock
                            ? SoundEvents.WOODEN_TRAPDOOR_OPEN
                            : SoundEvents.IRON_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
            level.gameEvent(player, GameEvent.BLOCK_OPEN, pos);
        }
    }

    private static void close(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.OPEN) || !state.getValue(BlockStateProperties.OPEN)) {
            return;
        }
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(player, level, state, pos, false);
            return;
        }
        level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, false), 10);
        level.gameEvent(player, GameEvent.BLOCK_CLOSE, pos);
    }

    private DoorLockInteractionHandler() {
    }
}
