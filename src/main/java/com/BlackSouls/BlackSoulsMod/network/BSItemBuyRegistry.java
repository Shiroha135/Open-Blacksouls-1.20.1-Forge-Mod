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

        register(BlackSouls.BLOOD_VIAL.get(), 200);
        register(BlackSouls.FIRE_BOMB.get(), 300);
        register(BlackSouls.THROWING_KNIFE.get(), 25);
        register(BlackSouls.ANTIDOTE_HERB.get(), 80);
        register(BlackSouls.HEMOSTATIC_CLOTH.get(), 100);
        register(BlackSouls.HOMEWARD_BONE_DUST.get(), 500);
        register(BlackSouls.MASTER_KEY.get(), 1000);
        register(BlackSouls.INVISIBLE_PEPPER.get(), 500);
        register(BlackSouls.FAIRY_SCALE_POWDER.get(), 200);
        register(BlackSouls.DUNG_PIE.get(), 100);
        register(BlackSouls.UPGRADE_SHARD.get(), 800);
        register(BlackSouls.MAGIC_STONE.get(), 250);
        register(BlackSouls.CHARCOAL_PINE_RESIN.get(), 800);
        register(BlackSouls.GOLD_PINE_RESIN.get(), 800);
        register(BlackSouls.DARK_PINE_RESIN.get(), 800);
        register(BlackSouls.ICE_PINE_RESIN.get(), 800);
        register(BlackSouls.UPGRADE_LARGE_SHARD.get(), 2000);
        register(BlackSouls.STAMINA_TONIC.get(), 500);
        register(BlackSouls.CANDLE_EMBER.get(), 500);
        register(BlackSouls.UPGRADE_CHUNK.get(), 4000);
        register(BlackSouls.GODDESS_BLOOD.get(), 8000);
    }

    public static void register(Item item, long price) {
        BUY_PRICES.put(item, price);
    }
}
