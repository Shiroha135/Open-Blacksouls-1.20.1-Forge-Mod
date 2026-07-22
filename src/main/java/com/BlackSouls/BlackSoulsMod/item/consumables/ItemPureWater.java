package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemPureWater extends Item {

    public ItemPureWater(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            boolean cured = false;
            if (BlackSouls.BUFF_BURN.isPresent() && player.hasEffect(BlackSouls.BUFF_BURN.get())) {
                player.removeEffect(BlackSouls.BUFF_BURN.get());
                player.clearFire();
                cured = true;
            }
            if (BlackSouls.BUFF_OILY.isPresent() && player.hasEffect(BlackSouls.BUFF_OILY.get())) {
                player.removeEffect(BlackSouls.BUFF_OILY.get());
                cured = true;
            }

            if (cured) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        BlackSouls.WATER1_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.blacksouls.pure_water.lore1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.pure_water.lore2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
