package com.BlackSouls.BlackSoulsMod.network;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.world.item.Item;
import java.util.HashMap;
import java.util.Map;

public class BSItemBuyRegistry {
    
    public static final Map<Item, Long> BUY_PRICES = new HashMap<>();

    public static void init() {
        
        register(BlackSouls.SOUL_GREEN.get(), 5000);
        register(BlackSouls.SOUL_PURPLE.get(), 5000);
        register(BlackSouls.SOUL_RED.get(), 5000);
        register(BlackSouls.SOUL_BLUE.get(), 5000);
        register(BlackSouls.SOUL_YELLOW.get(), 5000);
        register(BlackSouls.SOUL_GRAY.get(), 5000);
        register(BlackSouls.SOUL_WHITE.get(), 5000);
        register(BlackSouls.SOUL_FOUR_LEAF_CLOVER.get(), 5000);
        
        register(BlackSouls.RABBIT_WATCH.get(), 5000);

        
    }

    public static void register(Item item, long price) {
        BUY_PRICES.put(item, price);
    }
}
