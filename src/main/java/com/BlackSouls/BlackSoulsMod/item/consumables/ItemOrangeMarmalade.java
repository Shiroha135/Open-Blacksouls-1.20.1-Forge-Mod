package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemOrangeMarmalade extends Item {

    public ItemOrangeMarmalade(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (!player.level().isClientSide) {

            
            BSPlayerStats pStats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            double a_atk = pStats != null ? pStats.attack : player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double a_mat = pStats != null ? pStats.magicAttack : 0;

            
            
            BSPlayerStats tStats = target.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            double b_def = tStats != null ? tStats.defense : target.getAttributeValue(Attributes.ARMOR);
            double b_mdf = tStats != null ? tStats.magicDefense : target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

            
            
            double damage = 20.0 + (a_atk * 0.04) + (a_mat * 0.02) - (b_def * 0.02) - (b_mdf * 0.02);
            if (damage < 1.0) damage = 1.0; 

            
            target.hurt(player.damageSources().magic(), (float) damage);

            
            if (target.isAlive()) {
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_SEVERE_POISON.get(), 1000, 0));
            }

            target.level().playSound(null, target.blockPosition(), BlackSouls.EVASION1_EVENT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            target.level().playSound(null, target.blockPosition(), BlackSouls.POISON_EVENT.get(), SoundSource.PLAYERS, 0.8F, 0.7F);
            target.level().playSound(null, target.blockPosition(), BlackSouls.POISON_EVENT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            PacketPlayAnim animPacket = new PacketPlayAnim(205, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
            NetworkHandler.sendToAllAround(animPacket, target);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
