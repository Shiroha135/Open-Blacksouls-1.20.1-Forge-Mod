package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityThrownBlade;

public class ItemThrowingKnife extends ItemThrownBladeBase {
    public ItemThrowingKnife(Properties properties) {
        super(properties, EntityThrownBlade.MODE_THROWING_KNIFE, true, 0, 207,
                BlackSouls.DOWN2_EVENT, BlackSouls.SLASH11_EVENT, 3);
    }
}
