package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemSnakeGodBlood extends Item {

    public ItemSnakeGodBlood(Properties properties) {
        super(properties.stacksTo(99));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {

            if (BlackSouls.BUFF_REQUIEM.isPresent()) {
                player.addEffect(new MobEffectInstance(BlackSouls.BUFF_REQUIEM.get(), 600, 0));
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BlackSouls.SAND_EVENT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BlackSouls.SAINT7_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.snake_god_blood.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.snake_god_blood.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
