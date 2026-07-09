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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class ItemGoldenMead extends Item {
    public ItemGoldenMead(Item.Properties properties) {
        super(properties.stacksTo(64));
    }
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                BlackSouls.RAISE3_EVENT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                BlackSouls.ICE8_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_MADNESS.get(), -1, 0, false, true, true));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.consume(stack);
    }
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.blacksouls.golden_mead.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.golden_mead.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
