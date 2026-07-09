package com.BlackSouls.BlackSoulsMod.item.rings;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemRingWhiteRabbit extends ItemRingBase {

    public ItemRingWhiteRabbit(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);

        tooltip.add(Component.translatable("tooltip.blacksouls.ring_white_rabbit.action_count").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.blacksouls.ring_white_rabbit.all_stats").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}