package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityThrownBlade;

public class ItemScalpel extends ItemThrownBladeBase {
    public ItemScalpel(Properties properties) {
        super(properties, EntityThrownBlade.MODE_SCALPEL, false, 200, 207,
                BlackSouls.DOWN2_EVENT, BlackSouls.SLASH11_EVENT, 3);
    }
}
