package yuki135.blacksouls.item.consumables;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.EntityThrownBlade;

public class ItemThrowingKnife extends ItemThrownBladeBase {
    public ItemThrowingKnife(Properties properties) {
        super(properties, EntityThrownBlade.MODE_THROWING_KNIFE, true, 0, 207,
                BlackSouls.DOWN2_EVENT, BlackSouls.SLASH11_EVENT, 3);
    }
}
