package com.BlackSouls.BlackSoulsMod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemLoreBase extends Item {

    public ItemLoreBase(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        String baseName = this.getDescriptionId(stack);
        for (int i = 1; i <= 5; i++) {
            String loreKey = baseName + ".lore." + i;
            Component lore = Component.translatable(loreKey);
            if (!lore.getString().equals(loreKey)) {
                if (i == 1) {
                    tooltip.add(lore.copy().withStyle(ChatFormatting.WHITE));
                } else {
                    tooltip.add(lore.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
        }
    }
}
