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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ItemTinkerBellScalePowder extends Item {
    public ItemTinkerBellScalePowder(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            for (LivingEntity target : getAllies(player)) {
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_JUGGLING_EVASION.get(), 600, 0));
                level.playSound(null, target.blockPosition(), BlackSouls.WIND5_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                NetworkHandler.sendToAllAround(
                        new PacketPlayAnim(73, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()),
                        target
                );
                if (target instanceof Player targetPlayer) {
                    StatEventHandler.applyStats(targetPlayer);
                    StatEventHandler.syncToClient(targetPlayer);
                }
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.consume(stack);
    }

    private List<LivingEntity> getAllies(Player player) {
        List<LivingEntity> allies = new ArrayList<>();
        allies.add(player);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(15.0D))) {
            if (entity == player || !entity.isAlive()) {
                continue;
            }
            if (entity instanceof Player || entity instanceof TamableAnimal tamable && tamable.isTame()) {
                allies.add(entity);
            }
        }
        return allies;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
