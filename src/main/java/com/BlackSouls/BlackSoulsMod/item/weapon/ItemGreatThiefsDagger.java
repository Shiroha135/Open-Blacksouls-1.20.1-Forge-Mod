package com.BlackSouls.BlackSoulsMod.item.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemGreatThiefsDagger extends ItemThiefsDagger {

    public ItemGreatThiefsDagger(Properties properties) {
        super(properties);
    }

    @Override
    protected int[] getExtraHitTicks() {
        return new int[]{3, 5};
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.great_thiefs_dagger.lore.1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.blacksouls.great_thiefs_dagger.lore.2")
                .withStyle(ChatFormatting.WHITE));
    }
}
