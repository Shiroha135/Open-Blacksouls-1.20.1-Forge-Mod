package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalItemData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalItemTooltipHandler {
    private OriginalItemTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        BSOriginalItemData.Entry entry = BSOriginalItemData.get(event.getItemStack().getItem());
        if (entry == null) {
            return;
        }

        String categoryKey = entry.category() == BSOriginalItemData.Category.IMPORTANT
                ? "tooltip.blacksouls.item_category.important"
                : "tooltip.blacksouls.item_category.normal";
        ChatFormatting categoryColor = entry.category() == BSOriginalItemData.Category.IMPORTANT
                ? ChatFormatting.LIGHT_PURPLE
                : ChatFormatting.AQUA;

        event.getToolTip().add(Component.translatable(categoryKey).withStyle(categoryColor));
        event.getToolTip().add(Component.translatable("tooltip.blacksouls.item_price", entry.price())
                .withStyle(ChatFormatting.GOLD));
    }
}
