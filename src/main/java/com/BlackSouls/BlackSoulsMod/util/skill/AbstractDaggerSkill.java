package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

abstract class AbstractDaggerSkill extends AbstractWeaponCombatSkill {

    protected boolean isGreatDaggerOnly() {
        return false;
    }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            return false;
        }
        if (isGreatDaggerOnly()) {
            return mainHand.getItem() == BlackSouls.GREAT_THIEFS_DAGGER.get();
        }
        return mainHand.getItem() == BlackSouls.THIEFS_DAGGER.get()
                || mainHand.getItem() == BlackSouls.GREAT_THIEFS_DAGGER.get();
    }

    protected boolean applyHit(ServerPlayer player, LivingEntity target, BSPlayerStats stats,
                               double attackMultiplier, boolean canCrit, boolean sureHit,
                               double instantDeathRate) {
        return applyFormulaHit(player, target, stats, attackMultiplier, 2.0D, 0.2D,
                canCrit, sureHit, instantDeathRate);
    }
}
