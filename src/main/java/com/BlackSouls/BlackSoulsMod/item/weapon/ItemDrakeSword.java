package com.BlackSouls.BlackSoulsMod.item.weapon;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemDrakeSword extends ItemBSWeaponBase {

    public ItemDrakeSword(Properties properties) {
        super(Tiers.DIAMOND, 0, -2.4f, properties);

        this.stunChance = 0.10f; 
        this.stunDuration = 40;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {

            
            int upgradeLevel = stack.hasTag() ? stack.getTag().getInt("bs2_upgrade_level") : 0;

            
            double burnProb = 0.15;
            if (upgradeLevel == 1) burnProb = 0.30;
            else if (upgradeLevel == 2) burnProb = 0.40;
            else if (upgradeLevel == 3) burnProb = 0.60;
            else if (upgradeLevel == 4) burnProb = 0.70;
            else if (upgradeLevel >= 5) burnProb = 1.00;

            
            target.setSecondsOnFire(3);
            if (Math.random() < burnProb && BlackSouls.BUFF_BURN.isPresent()) {
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_BURN.get(), 60, 0));
            }

            if (attacker instanceof ServerPlayer player) {
                PacketPlayAnim animPacket = new PacketPlayAnim(169, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
                NetworkHandler.sendToAllAround(animPacket, target);

                net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
                server.tell(new net.minecraft.server.TickTask(0, () -> {
                    if (!target.isRemoved()) {
                        playWeaponSound(target, BlackSouls.SLASH1_EVENT.get(), 1.0f, 1.0f);
                    }
                }));
                server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(132 / 50.0)), () -> {
                    if (!target.isRemoved()) {
                        playWeaponSound(target, BlackSouls.FIRE3_EVENT.get(), 1.0f, 1.0f);
                    }
                }));
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    private void playWeaponSound(LivingEntity target, SoundEvent sound, float volume, float pitch) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.drake_sword.lore.1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        int upgradeLevel = stack.hasTag() ? stack.getTag().getInt("bs2_upgrade_level") : 0;

        int burnRate = 15;
        if (upgradeLevel == 1) burnRate = 30;
        else if (upgradeLevel == 2) burnRate = 40;
        else if (upgradeLevel == 3) burnRate = 60;
        else if (upgradeLevel == 4) burnRate = 70;
        else if (upgradeLevel >= 5) burnRate = 100;

        String skillKey = upgradeLevel >= 5 ? "skill.blacksouls.hellfire_blade_combo.name" : "skill.blacksouls.bs2_skill_dragon_shockwave.name";

        
        tooltip.add(Component.translatable("item.blacksouls.drake_sword.lore.dynamic",
                        burnRate,
                        Component.translatable(skillKey))
                .withStyle(ChatFormatting.WHITE));

        super.appendHoverText(stack, level, tooltip, flagIn);
    }
}
