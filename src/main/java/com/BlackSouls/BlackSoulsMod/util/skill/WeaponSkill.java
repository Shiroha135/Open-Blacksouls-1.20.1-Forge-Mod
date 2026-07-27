package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public abstract class WeaponSkill extends AbstractSkill {

    protected abstract boolean isWeaponEquipped(Player player);

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return isWeaponEquipped(player);
    }

    @Override
    public boolean canCast(ServerPlayer player, BSPlayerStats stats) {
        if (!isWeaponEquipped(player)) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.wrong_weapon").withStyle(ChatFormatting.RED));
            return false;
        }
        return super.canCast(player, stats);
    }

    @Override
    public boolean canCastInTurnBattle(ServerPlayer player, BSPlayerStats stats) {
        if (!isWeaponEquipped(player)) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.wrong_weapon").withStyle(ChatFormatting.RED));
            return false;
        }
        return super.canCastInTurnBattle(player, stats);
    }
}
