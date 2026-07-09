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

public class ItemVorpalBlade extends ItemBSWeaponBase {

    public ItemVorpalBlade(Properties properties) {
        super(Tiers.DIAMOND, 0, -2.4f, properties);

        this.stunChance = 0.15f;
        this.stunDuration = 50;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.vorpal_blade.lore.1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        tooltip.add(Component.translatable("item.blacksouls.vorpal_blade.lore.2")
                .withStyle(ChatFormatting.WHITE));

        super.appendHoverText(stack, level, tooltip, flagIn);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof ServerPlayer player) {

            PacketPlayAnim animPacket = new PacketPlayAnim(242, target.getX(), target.getY(), target.getZ());
            NetworkHandler.sendToAllAround(animPacket, target);

            net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
            server.tell(new net.minecraft.server.TickTask(0, () -> {
                if (!target.isRemoved()) {
                    playWeaponSound(target, BlackSouls.ATTACK3_EVENT.get(), 1.0f, 1.0f);
                }
            }));
            server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(330 / 50.0)), () -> {
                if (!target.isRemoved()) {
                    playWeaponSound(target, BlackSouls.SLASH8_EVENT.get(), 1.0f, 1.0f);
                }
            }));
            server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round((330 + 66) / 50.0)), () -> {
                if (!target.isRemoved()) {
                    playWeaponSound(target, BlackSouls.SLASH3_EVENT.get(), 1.0f, 1.0f);
                }
            }));
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    private void playWeaponSound(LivingEntity target, SoundEvent sound, float volume, float pitch) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
