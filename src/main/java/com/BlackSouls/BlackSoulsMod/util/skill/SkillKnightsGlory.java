package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SkillKnightsGlory extends WeaponSkill {

    @Override
    public String getSkillId() { return "bs2_skill_knights_glory"; }

    @Override
    public float getManaCost() { return 40.0f; } 

    @Override
    public int getBaseCooldownTicks() { return 4000; } 

    @Override
    public String getTranslationKey() { return "skill.blacksouls.knights_glory.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        return !mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KNIGHT_KING_SWORD.get();
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
      
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.knights_glory.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));

        PacketPlayAnim animPacket = new PacketPlayAnim(33, player.getX(), player.getY() + player.getBbHeight() / 2.0F, player.getZ());
        NetworkHandler.sendToAllAround(animPacket, player);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), BlackSouls.SAINT7_EVENT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        float healAmount = player.getMaxHealth() * 0.5f;
        player.heal(healAmount);

        if (BlackSouls.BUFF_KNIGHTS_GLORY.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_KNIGHTS_GLORY.get(), 2000, 0));
        }
    }
}