package com.BlackSouls.BlackSoulsMod.item.consumables;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemUnavailableStoryConsumable extends ItemSimpleLore {
    private final String messageKey;

    public ItemUnavailableStoryConsumable(Properties properties, String messageKey) {
        super(properties, 2);
        this.messageKey = messageKey;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResultHolder.fail(stack);
    }
}
