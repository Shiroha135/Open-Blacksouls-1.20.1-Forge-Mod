package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;
import java.util.List;

public class ItemMermaidSong extends Item {
    public ItemMermaidSong(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                BlackSouls.SONG_EVENT.get(), SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);

        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_HELANRITH_WINE.get(), 1200, 0));
        }

        player.getCooldowns().addCooldown(this, 10);

        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.blacksouls.mermaid_song.lore1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.mermaid_song.lore2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}