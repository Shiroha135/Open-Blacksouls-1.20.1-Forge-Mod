package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

abstract class AbstractBandersnatchSkill extends AbstractWeaponCombatSkill {

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        return !mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BANDERSNATCH_SWORD.get();
    }
}
