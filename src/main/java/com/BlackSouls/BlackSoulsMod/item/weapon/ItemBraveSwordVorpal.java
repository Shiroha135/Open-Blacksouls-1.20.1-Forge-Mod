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

public class ItemBraveSwordVorpal extends ItemBSWeaponBase {

    public ItemBraveSwordVorpal(Properties properties) {
        super(Tiers.DIAMOND, 0, -2.4f, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.brave_sword_vorpal.lore.1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.blacksouls.brave_sword_vorpal.lore.2")
                .withStyle(ChatFormatting.WHITE));
        super.appendHoverText(stack, level, tooltip, flagIn);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof ServerPlayer player) {

            PacketPlayAnim animPacket = new PacketPlayAnim(249, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
            NetworkHandler.sendToAllAround(animPacket, target);

            net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
            server.tell(new net.minecraft.server.TickTask(0, () -> {
                if (!target.isRemoved()) {
                    playWeaponSound(target, BlackSouls.SLASH1_EVENT.get(), 1.0f, 1.0f);
                    playWeaponSound(target, BlackSouls.EVASION1_EVENT.get(), 1.0f, 1.0f);
                }
            }));
            server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(300 / 50.0)), () -> {
                if (!target.isRemoved()) {
                    playWeaponSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0f, 1.0f);
                    playWeaponSound(target, BlackSouls.DAO_EVENT.get(), 1.0f, 1.0f);
                }
            }));
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    private void playWeaponSound(LivingEntity target, SoundEvent sound, float volume, float pitch) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
