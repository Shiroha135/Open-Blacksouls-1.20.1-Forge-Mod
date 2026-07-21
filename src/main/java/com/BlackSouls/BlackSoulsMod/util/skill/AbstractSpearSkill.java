package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

abstract class AbstractSpearSkill extends AbstractWeaponCombatSkill {

    protected boolean isGungnirOnly() {
        return false;
    }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return false;
        if (isGungnirOnly()) {
            return mainHand.getItem() == BlackSouls.GUNGNIR.get();
        }
        return mainHand.getItem() == BlackSouls.BROAD_SPEAR.get()
                || mainHand.getItem() == BlackSouls.GUNGNIR.get();
    }
}
