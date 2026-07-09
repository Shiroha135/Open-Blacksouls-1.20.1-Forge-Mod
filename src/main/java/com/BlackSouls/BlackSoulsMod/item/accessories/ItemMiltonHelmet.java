package com.BlackSouls.BlackSoulsMod.item.accessories;

import com.BlackSouls.BlackSoulsMod.item.ItemBaubleBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemMiltonHelmet extends ItemBaubleBase {

    public ItemMiltonHelmet(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.blacksouls.milton_helmet.defense").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.blacksouls.milton_helmet.fire_resist").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.blacksouls.milton_helmet.burn_resist").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.blacksouls.milton_helmet.speed").withStyle(ChatFormatting.RED));
    }
}
