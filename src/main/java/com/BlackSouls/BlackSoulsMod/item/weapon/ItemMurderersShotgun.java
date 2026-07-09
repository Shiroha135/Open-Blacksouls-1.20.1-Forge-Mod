package com.BlackSouls.BlackSoulsMod.item.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemMurderersShotgun extends Item {

    public final double attackDamage = 50.0;

    public ItemMurderersShotgun(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.murderers_shotgun.lore1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.murderers_shotgun.lore2").withStyle(ChatFormatting.GRAY));
    }
}