package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

abstract class AbstractGreatSwordSkill extends AbstractWeaponCombatSkill {

    protected boolean isGiantSwordOnly() {
        return false;
    }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return false;
        if (isGiantSwordOnly()) {
            return mainHand.getItem() == BlackSouls.GIANT_SWORD.get();
        }
        return mainHand.getItem() == BlackSouls.GREAT_SWORD.get()
                || mainHand.getItem() == BlackSouls.GIANT_SWORD.get();
    }
}
