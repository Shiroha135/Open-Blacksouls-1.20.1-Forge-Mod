package com.BlackSouls.BlackSoulsMod.item.accessories;

import com.BlackSouls.BlackSoulsMod.item.ItemBaubleBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemGuardianAngel extends ItemBaubleBase {

    public ItemGuardianAngel(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);

        tooltip.add(Component.translatable("tooltip.blacksouls.guardian_angel.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.blacksouls.guardian_angel.defense").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.blacksouls.guardian_angel.magic_defense").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.blacksouls.guardian_angel.revive").withStyle(ChatFormatting.GOLD));
    }
}