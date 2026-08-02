package com.BlackSouls.BlackSoulsMod.item.weapon;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ItemKnightShield extends ShieldItem {
    public ItemKnightShield(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(getDescriptionId() + ".stat.1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(getDescriptionId() + ".stat.2").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(getDescriptionId() + ".stat.3").withStyle(ChatFormatting.AQUA));
    }
}
