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

public class ItemSedative extends Item {

    public ItemSedative(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide()) {
            boolean cured = false;
            if (BlackSouls.BUFF_MADNESS.isPresent() && player.hasEffect(BlackSouls.BUFF_MADNESS.get())) {
                player.removeEffect(BlackSouls.BUFF_MADNESS.get());
                player.invulnerableTime = 0;
                cured = true;
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BlackSouls.RAISE3_EVENT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BlackSouls.ICE8_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            if (cured) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 0.5F);
            }

            player.getCooldowns().addCooldown(this, 10);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}