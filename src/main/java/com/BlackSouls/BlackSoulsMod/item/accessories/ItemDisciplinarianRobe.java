package com.BlackSouls.BlackSoulsMod.item.accessories;

import com.BlackSouls.BlackSoulsMod.item.ItemBaubleBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemDisciplinarianRobe extends ItemBaubleBase {

    public ItemDisciplinarianRobe(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.blacksouls.disciplinarian_robe.magic_defense").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.blacksouls.disciplinarian_robe.mp_regen").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.blacksouls.disciplinarian_robe.speed").withStyle(ChatFormatting.RED));
    }
}
