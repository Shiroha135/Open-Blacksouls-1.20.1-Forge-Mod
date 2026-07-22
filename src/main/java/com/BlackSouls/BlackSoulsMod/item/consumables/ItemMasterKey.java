package com.BlackSouls.BlackSoulsMod.item.consumables;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemMasterKey extends Item {
    public ItemMasterKey(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!state.hasProperty(BlockStateProperties.OPEN) || state.getValue(BlockStateProperties.OPEN)) {
            return InteractionResult.PASS;
        }

        boolean unlocked = false;
        if (state.is(Blocks.IRON_DOOR) && state.getBlock() instanceof DoorBlock door) {
            if (!level.isClientSide()) {
                door.setOpen(context.getPlayer(), level, state, pos, true);
            }
            unlocked = true;
        } else if (state.is(Blocks.IRON_TRAPDOOR)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, true), 10);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(context.getPlayer(), GameEvent.BLOCK_OPEN, pos);
            }
            unlocked = true;
        }

        if (!unlocked) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
