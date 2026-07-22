package com.BlackSouls.BlackSoulsMod.item.consumables;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemSimpleLore extends Item {
    private final int loreLines;

    public ItemSimpleLore(Properties properties, int loreLines) {
        super(properties);
        this.loreLines = loreLines;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        for (int i = 1; i <= loreLines; i++) {
            Component line = Component.translatable(this.getDescriptionId() + ".lore." + i);
            tooltip.add(i == 1 ? line.copy().withStyle(ChatFormatting.WHITE) : line.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
