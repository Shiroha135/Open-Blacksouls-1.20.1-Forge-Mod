package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

abstract class AbstractClubSkill extends AbstractWeaponCombatSkill {

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        return !mainHand.isEmpty()
                && (mainHand.getItem() == BlackSouls.CLUB.get()
                || mainHand.getItem() == BlackSouls.KING_CLUB.get());
    }
}
