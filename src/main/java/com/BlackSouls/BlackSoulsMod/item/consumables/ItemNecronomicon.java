package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemNecronomicon extends Item {
    public ItemNecronomicon(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_NECRONOMICON.get(), 2000, 0));
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_MADNESS.get(), 2000, 0));
            level.playSound(null, player.blockPosition(), BlackSouls.BLIND_EVENT.get(), SoundSource.PLAYERS, 1.0F, 0.75F);
            NetworkHandler.sendToAllAround(
                    new PacketPlayAnim(51, player.getX(), player.getY() + player.getBbHeight() / 2.0F, player.getZ()),
                    player
            );
            StatEventHandler.applyStats(player);
            StatEventHandler.syncToClient(player);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
