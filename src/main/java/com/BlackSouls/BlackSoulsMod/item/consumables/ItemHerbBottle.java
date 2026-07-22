package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.item.ItemLoreBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemHerbBottle extends ItemLoreBase {

    public ItemHerbBottle(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.herb_bottle.empty").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResultHolder.fail(stack);
        }

        
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        
        if (!level.isClientSide()) {
            
            float healAmount = (float) (player.getMaxHealth() * 0.50F * StatEventHandler.getItemRecoveryMultiplier(player));
            player.heal(healAmount);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BlackSouls.ICE1_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            
            stack.setDamageValue(stack.getDamageValue() + 1);
            player.getCooldowns().addCooldown(this, 10);
        }

        return InteractionResultHolder.consume(stack);

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);
        
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        ChatFormatting color = (remaining <= 3) ? ChatFormatting.RED : ChatFormatting.GREEN;

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.blacksouls.remaining_uses", remaining, stack.getMaxDamage()).withStyle(color));
        tooltip.add(Component.translatable("tooltip.blacksouls.bonfire_refill").withStyle(ChatFormatting.GRAY));
    }
}
