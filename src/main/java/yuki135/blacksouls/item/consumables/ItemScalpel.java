package yuki135.blacksouls.item.consumables;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.EntityThrownBlade;

public class ItemScalpel extends ItemThrownBladeBase {
    public ItemScalpel(Properties properties) {
        super(properties, EntityThrownBlade.MODE_SCALPEL, false, 200, 207,
                BlackSouls.DOWN2_EVENT, BlackSouls.SLASH11_EVENT, 3);
    }
}
