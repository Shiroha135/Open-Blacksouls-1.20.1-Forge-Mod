package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemParameterBoost extends Item {
    public enum Mode {
        SPEED,
        LUCK,
        HP_MP
    }

    private final Mode mode;

    public ItemParameterBoost(Properties properties, Mode mode) {
        super(properties);
        this.mode = mode;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            apply(player, player, stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }
        if (!player.level().isClientSide()) {
            apply(player, target, stack);
        }
        return InteractionResult.CONSUME;
    }

    private void apply(Player user, LivingEntity target, ItemStack stack) {
        if (mode == Mode.SPEED) {
            StatEventHandler.applySpeedUp(target, 1000);
        } else if (mode == Mode.LUCK) {
            StatEventHandler.applyLuckUp(target, 1000);
        } else {
            target.addEffect(new MobEffectInstance(BlackSouls.BUFF_HP_MP_UP.get(), 1000, 0));
        }

        user.level().playSound(null, target.blockPosition(), BlackSouls.UP4_EVENT.get(), SoundSource.PLAYERS, 0.8F, 1.1F);
        NetworkHandler.sendToAllAround(
                new PacketPlayAnim(208, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()),
                target
        );

        if (target instanceof Player targetPlayer) {
            StatEventHandler.applyStats(targetPlayer);
            StatEventHandler.syncToClient(targetPlayer);
        }
        if (!user.getAbilities().instabuild) {
            stack.shrink(1);
        }
        user.getCooldowns().addCooldown(this, 10);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
