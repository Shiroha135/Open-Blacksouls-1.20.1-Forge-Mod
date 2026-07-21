package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SkillReinforce extends WeaponSkill {

    @Override
    public String getSkillId() { return "bs2_skill_reinforce"; }

    @Override
    public float getManaCost() { return 10.0f; }

    @Override
    public int getBaseCooldownTicks() { return 200; } 

    @Override
    public String getTranslationKey() { return "skill.blacksouls.reinforce.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        return !mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.BRAVE_SWORD_VORPAL.get()
                || mainHand.getItem() == BlackSouls.VORPAL_BLADE.get()
                || mainHand.getItem() == BlackSouls.VORPAL_SWORD.get());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.reinforce.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));

        PacketPlayAnim animPacket = new PacketPlayAnim(335, player.getX(), player.getY() + player.getBbHeight() / 2.0F, player.getZ());
        NetworkHandler.sendToAllAround(animPacket, player);

        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(0, () -> {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), BlackSouls.DARKNESS3_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), BlackSouls.FIRE4_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        }));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(66 / 50.0)), () -> {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), BlackSouls.THUNDER5_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        }));

        int duration = 2000;
        if (BlackSouls.BUFF_FIRE_POWER.isPresent()) player.addEffect(new MobEffectInstance(BlackSouls.BUFF_FIRE_POWER.get(), duration, 0, false, false, true));
        if (BlackSouls.BUFF_ICE_POWER.isPresent()) player.addEffect(new MobEffectInstance(BlackSouls.BUFF_ICE_POWER.get(), duration, 0, false, false, true));
        if (BlackSouls.BUFF_THUNDER_POWER.isPresent()) player.addEffect(new MobEffectInstance(BlackSouls.BUFF_THUNDER_POWER.get(), duration, 0, false, false, true));
        if (BlackSouls.BUFF_DARK_POWER.isPresent()) player.addEffect(new MobEffectInstance(BlackSouls.BUFF_DARK_POWER.get(), duration, 0, false, false, true));

        player.sendSystemMessage(Component.translatable("message.blacksouls.buff.fire_power", player.getName().getString()).withStyle(ChatFormatting.RED));
        player.sendSystemMessage(Component.translatable("message.blacksouls.buff.ice_power", player.getName().getString()).withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.translatable("message.blacksouls.buff.thunder_power", player.getName().getString()).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("message.blacksouls.buff.dark_power", player.getName().getString()).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
