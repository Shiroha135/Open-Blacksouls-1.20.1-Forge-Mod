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

public class ItemAndorSword extends ItemBSWeaponBase {

    public ItemAndorSword(Properties properties) {
        super(Tiers.DIAMOND, 0, -2.4f, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.andor_sword.lore.1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        int upgradeLevel = 0;
        if (stack.hasTag() && stack.getTag().contains("bs2_upgrade_level")) {
            upgradeLevel = stack.getTag().getInt("bs2_upgrade_level");
        }

        String skillKey = upgradeLevel >= 5 ? "skill.blacksouls.aura_blade_radiant.name" : "skill.blacksouls.bs2_skill_aura_blade.name";

        
        int extraActionRate = 30;
        if (upgradeLevel == 2) extraActionRate = 50;
        else if (upgradeLevel == 3) extraActionRate = 70;
        else if (upgradeLevel == 4) extraActionRate = 90;
        else if (upgradeLevel >= 5) extraActionRate = 100;

        tooltip.add(Component.translatable("item.blacksouls.andor_sword.lore.dynamic",
                        Component.translatable(skillKey),
                        extraActionRate)
                .withStyle(ChatFormatting.WHITE));

        super.appendHoverText(stack, level, tooltip, flagIn);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof ServerPlayer player) {
            PacketPlayAnim animPacket = new PacketPlayAnim(132, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
            NetworkHandler.sendToAllAround(animPacket, target);
            playWeaponSound(target, BlackSouls.SLASH3_EVENT.get(), 1.0f, 1.0f);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    private void playWeaponSound(LivingEntity target, SoundEvent sound, float volume, float pitch) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
