package com.BlackSouls.BlackSoulsMod.item.rings;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemRingMidnightCrown extends ItemRingBase {
    public ItemRingMidnightCrown(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);
        tooltip.add(Component.translatable("tooltip.blacksouls.ring_midnight_crown.hp").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.blacksouls.ring_midnight_crown.mp").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.blacksouls.ring_midnight_crown.magic_attack").withStyle(ChatFormatting.AQUA));
    }
}
