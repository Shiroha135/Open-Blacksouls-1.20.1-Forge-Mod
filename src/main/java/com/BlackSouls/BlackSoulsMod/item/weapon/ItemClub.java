package com.BlackSouls.BlackSoulsMod.item.weapon;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemClub extends ItemBSWeaponBase {

    public ItemClub(Properties properties) {
        super(Tiers.DIAMOND, 0, -2.4F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof ServerPlayer player) {
            playAttackEffects(player, target);
            applyCrush(target, isStrongCrush());
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    protected boolean isStrongCrush() {
        return false;
    }

    public static void playAttackEffects(ServerPlayer player, LivingEntity target) {
        NetworkHandler.sendToAllAround(new PacketPlayAnim(
                212,
                target.getX(),
                target.getY() + target.getBbHeight() / 2.0F,
                target.getZ()
        ), target);
        playSound(target, BlackSouls.WIND7_EVENT.get(), 0.5F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(4, () -> {
            if (!target.isRemoved()) {
                playSound(target, BlackSouls.BLOW7_EVENT.get(), 0.5F);
            }
        }));
    }

    public static void applyCrush(LivingEntity target, boolean strong) {
        if (BlackSouls.BUFF_DEFENSELESS.isPresent()) {
            target.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSELESS.get(), 400, strong ? 1 : 0));
        }
        if (BlackSouls.BUFF_STUN.isPresent()) {
            target.addEffect(new MobEffectInstance(BlackSouls.BUFF_STUN.get(), 40, 0));
        }
    }

    private static void playSound(LivingEntity target, SoundEvent sound, float pitch) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 1.0F, pitch);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.club.lore.1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.blacksouls.club.lore.2")
                .withStyle(ChatFormatting.WHITE));
        super.appendHoverText(stack, level, tooltip, flagIn);
    }
}
