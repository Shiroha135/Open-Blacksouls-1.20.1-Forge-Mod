package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ItemHelanrithWine extends Item {
    public ItemHelanrithWine(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BlackSouls.RAISE3_EVENT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BlackSouls.ICE8_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_HELANRITH_WINE.get(), 1200, 0));
            player.getCooldowns().addCooldown(this, 10);
        }

        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.blacksouls.helanrith_wine.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.helanrith_wine.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
