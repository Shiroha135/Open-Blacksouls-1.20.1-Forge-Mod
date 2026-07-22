package com.BlackSouls.BlackSoulsMod.network;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BSItemSellRegistry {
    public static class SellInfo {
        public final long price;
        public final String descKey;

        public SellInfo(long price, String descKey) {
            this.price = price;
            this.descKey = descKey;
        }
    }

    public static final Map<Item, SellInfo> SELL_PRICES = new HashMap<>();

    public static void init() {
        register(BlackSouls.BLOOD_VIAL.get(), 100, "lore.blacksouls.blood_vial");
        register(BlackSouls.SOUL_GREEN.get(), 2500, "lore.blacksouls.soul_green");
        register(BlackSouls.SOUL_PURPLE.get(), 2500, "lore.blacksouls.soul_purple");
        register(BlackSouls.SOUL_RED.get(), 2500, "lore.blacksouls.soul_red");
        register(BlackSouls.SOUL_BLUE.get(), 2500, "lore.blacksouls.soul_blue");
        register(BlackSouls.SOUL_YELLOW.get(), 2500, "lore.blacksouls.soul_yellow");
        register(BlackSouls.SOUL_GRAY.get(), 2500, "lore.blacksouls.soul_gray");
        register(BlackSouls.SOUL_WHITE.get(), 2500, "lore.blacksouls.soul_white");
        register(BlackSouls.SOUL_FOUR_LEAF_CLOVER.get(), 2500, "lore.blacksouls.soul_four_leaf_clover");
        register(BlackSouls.RABBIT_WATCH.get(), 2500, "lore.blacksouls.rabbit_watch");
        register(BlackSouls.MAIDENSFRAGRANCE.get(), 1250, "lore.blacksouls.maidensfragrance");
        register(BlackSouls.ABANDONED_TRASH.get(), 1, "lore.blacksouls.abandoned_trash");
        register(BlackSouls.SOUL_FADING.get(), 50, "lore.blacksouls.soul_fading");
        register(BlackSouls.SOUL_LOST_UNDEAD.get(), 200, "lore.blacksouls.soul_lost_undead");
        register(BlackSouls.SOUL_LOST_UNDEAD_LARGE.get(), 400, "lore.blacksouls.soul_lost_undead_large");
        register(BlackSouls.SOUL_NAMELESS_TRAVELER.get(), 800, "lore.blacksouls.soul_nameless_traveler");
        register(BlackSouls.SOUL_NAMELESS_TRAVELER_LARGE.get(), 1000, "lore.blacksouls.soul_nameless_traveler_large");
        register(BlackSouls.SOUL_NAMELESS_SOLDIER.get(), 2000, "lore.blacksouls.soul_nameless_soldier");
        register(BlackSouls.SOUL_NAMELESS_SOLDIER_LARGE.get(), 3000, "lore.blacksouls.soul_nameless_soldier_large");
        register(BlackSouls.SOUL_EXHAUSTED_WARRIOR.get(), 5000, "lore.blacksouls.soul_exhausted_warrior");
        register(BlackSouls.SOUL_EXHAUSTED_WARRIOR_LARGE.get(), 8000, "lore.blacksouls.soul_exhausted_warrior_large");
        register(BlackSouls.SOUL_CRESTFALLEN_KNIGHT.get(), 10000, "lore.blacksouls.soul_crestfallen_knight");
        register(BlackSouls.SOUL_CRESTFALLEN_KNIGHT_LARGE.get(), 20000, "lore.blacksouls.soul_crestfallen_knight_large");
        register(BlackSouls.BLACKWELL_BLOOD_VIAL.get(), 500, "lore.blacksouls.blackwell_blood_vial");
        register(BlackSouls.PURE_WATER.get(), 25, "lore.blacksouls.pure_water");
        register(BlackSouls.GOLDENMEAD.get(), 750, "lore.blacksouls.golden_mead");
        register(BlackSouls.MERMAIDSONG.get(), 1500, "lore.blacksouls.mermaidong");
        register(BlackSouls.HELANRITHWINE.get(), 450000, "lore.blacksouls.helanrith_wine");
        register(BlackSouls.GODDESS_BLOOD.get(),4000,"lore.blacksouls.goddess_blood");
        register(BlackSouls.PIGEON_EGG.get(),250,"lore.blacksouls.pigeon_egg");
        register(BlackSouls.SOUL_HERO.get(),25000,"lore.blacksouls.soul_hero");
        register(BlackSouls.SOUL_GREAT_HERO.get(),50000,"lore.blacksouls.soul_great_hero");
        register(BlackSouls.MATCH_MEDICINE.get(),500,"lore.blacksouls.match_medicine");
        register(BlackSouls.CHICKEN.get(),1500,"lore.blacksouls.chicken");
        register(BlackSouls.CHRISTMAS_CHICKEN.get(),1500,"lore.blacksouls.christmas_chicken");
        register(BlackSouls.MYSTERIOUS_MEAT.get(),1,"lore.blacksouls.mysterious_meat");
        register(BlackSouls.BILLS_BENTO.get(),250,"lore.blacksouls.bills_bento");
        register(BlackSouls.SNAKE_GOD_BLOOD.get(),1000,"lore.blacksouls.snake_god_blood");
        register(BlackSouls.GOAT_MEAT.get(),4000,"lore.blacksouls.goat_meat");
        register(BlackSouls.PREGNANT_CAKE_MEAT.get(),4000,"lore.blacksouls.pregnant_cake_meat");
        register(BlackSouls.FAIRY_FEATHER.get(), 150);
        register(BlackSouls.GOLDEN_EGG.get(), 250000);
        register(BlackSouls.NIGHTMARE_LANTERN.get(), 500);
        register(BlackSouls.SATYRS_THING.get(), 1);
        register(BlackSouls.SQUIRREL_FUR.get(), 250);
        register(BlackSouls.FILTHY_LIQUID.get(), 150);
        register(BlackSouls.BLUEBIRD_FEATHER.get(), 150);
        register(BlackSouls.TINKER_BELLS_SCALE_POWDER.get(), 5000);
        register(BlackSouls.OUIJA_BOARD.get(), 5000);
        register(BlackSouls.ROLDS_FOUNTAIN_PEN.get(), 500);
        register(BlackSouls.COLD_VALLEY_BREATH.get(), 1000);
        register(BlackSouls.NECRONOMICON.get(), 150000);
    }

    private static void register(Item item, long price, String descKey) {
        SELL_PRICES.put(item, new SellInfo(price, descKey));
    }

    public static void register(Item item, long price) {
        SELL_PRICES.put(item, new SellInfo(price, ""));
    }

    public static List<String> getDefaultLoreKeys(Item item) {
        List<String> keys = new ArrayList<>();
        String baseKey = item.getDescriptionId();
        keys.add(baseKey + ".lore.1");
        keys.add(baseKey + ".lore.2");
        keys.add(baseKey + ".lore.3");
        keys.add(baseKey + ".lore");
        return keys;
    }
}
