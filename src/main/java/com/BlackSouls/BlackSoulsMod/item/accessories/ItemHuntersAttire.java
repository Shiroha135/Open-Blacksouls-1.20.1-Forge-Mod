package com.BlackSouls.BlackSoulsMod.item.accessories;

import com.BlackSouls.BlackSoulsMod.item.ItemBaubleBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class ItemHuntersAttire extends ItemBaubleBase implements ICurioItem {

    public ItemHuntersAttire(Properties properties) {
        super(properties);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {

            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getStacksHandler("head").ifPresent(headHandler -> {

                    for (int i = 0; i < headHandler.getSlots(); i++) {
                        ItemStack headItem = headHandler.getStacks().getStackInSlot(i);

                        if (!headItem.isEmpty()) {
                            headHandler.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                            if (!player.getInventory().add(headItem)) {
                                player.drop(headItem, true);
                            }

                            player.displayClientMessage(Component.translatable("message.blacksouls.hunters_attire.conflict").withStyle(ChatFormatting.RED), false);
                        }
                    }
                });
            });
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.blacksouls.hunters_attire.dodge").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.blacksouls.hunters_attire.speed").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.blacksouls.hunters_attire.no_head").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
    }
}