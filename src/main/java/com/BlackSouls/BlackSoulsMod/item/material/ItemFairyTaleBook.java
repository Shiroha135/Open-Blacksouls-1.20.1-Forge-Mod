package com.BlackSouls.BlackSoulsMod.item.material;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class ItemFairyTaleBook extends Item {
    private final String[] loreKeys;
    public ItemFairyTaleBook(Properties properties, String... loreKeys) {
        super(properties);
        this.loreKeys = loreKeys != null ? loreKeys : new String[0];
    }
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        for (String key : loreKeys) {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}