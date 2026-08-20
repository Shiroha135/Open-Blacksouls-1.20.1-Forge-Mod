package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("removal")
public final class BSOriginalItemData {
    public enum Category {
        NORMAL,
        IMPORTANT
    }

    public record Entry(Category category, int price, boolean consumable, int occasion, int scope) {
        public boolean canUseInBattle() {
            return category == Category.NORMAL && (occasion == 0 || occasion == 1) && scope != 0;
        }
    }

    private static final Map<String, Entry> ENTRIES = createEntries();

    private BSOriginalItemData() {}

    private static Map<String, Entry> createEntries() {
        Map<String, Entry> entries = new LinkedHashMap<>();
        entries.put("herb_bottle", new Entry(Category.NORMAL, 0, true, 0, 7));
        entries.put("herb_bottle_m", new Entry(Category.NORMAL, 0, true, 0, 7));
        entries.put("blood_vial", new Entry(Category.NORMAL, 200, true, 0, 7));
        entries.put("antidote_herb", new Entry(Category.NORMAL, 80, true, 0, 7));
        entries.put("hemostatic_cloth", new Entry(Category.NORMAL, 100, true, 0, 7));
        entries.put("sedative", new Entry(Category.NORMAL, 200, true, 0, 7));
        entries.put("pigeon_egg", new Entry(Category.NORMAL, 500, true, 0, 7));
        entries.put("goddess_blood", new Entry(Category.NORMAL, 8000, true, 0, 7));
        entries.put("soul_green", new Entry(Category.NORMAL, 5000, false, 2, 0));
        entries.put("soul_purple", new Entry(Category.NORMAL, 5000, false, 2, 0));
        entries.put("soul_red", new Entry(Category.NORMAL, 5000, false, 2, 0));
        entries.put("soul_blue", new Entry(Category.NORMAL, 5000, false, 2, 0));
        entries.put("soul_yellow", new Entry(Category.NORMAL, 5000, false, 2, 0));
        entries.put("soul_gray", new Entry(Category.NORMAL, 5000, false, 2, 0));
        entries.put("soul_white", new Entry(Category.NORMAL, 5000, false, 2, 0));
        entries.put("soul_four_leaf_clover", new Entry(Category.NORMAL, 5000, false, 2, 0));
        entries.put("soul_black", new Entry(Category.NORMAL, 0, false, 2, 0));
        entries.put("homeward_bone_dust", new Entry(Category.NORMAL, 500, true, 2, 7));
        entries.put("rabbit_watch", new Entry(Category.NORMAL, 5000, true, 1, 7));
        entries.put("invisible_pepper", new Entry(Category.NORMAL, 500, true, 2, 11));
        entries.put("abandoned_trash", new Entry(Category.NORMAL, 2, false, 3, 7));
        entries.put("magic_stone", new Entry(Category.NORMAL, 250, true, 1, 7));
        entries.put("maidens_fragrance", new Entry(Category.NORMAL, 2500, true, 0, 7));
        entries.put("fairy_scale_powder", new Entry(Category.NORMAL, 200, true, 1, 1));
        entries.put("mysterious_shard", new Entry(Category.NORMAL, 5000, true, 3, 7));
        entries.put("upgrade_shard", new Entry(Category.NORMAL, 800, true, 3, 7));
        entries.put("upgrade_large_shard", new Entry(Category.NORMAL, 2000, true, 3, 7));
        entries.put("upgrade_chunk", new Entry(Category.NORMAL, 4000, true, 3, 7));
        entries.put("upgrade_slab", new Entry(Category.NORMAL, 10000, true, 3, 7));
        entries.put("fire_bomb", new Entry(Category.NORMAL, 300, true, 1, 1));
        entries.put("dung_pie", new Entry(Category.NORMAL, 100, true, 1, 1));
        entries.put("charcoal_pine_resin", new Entry(Category.NORMAL, 800, true, 1, 11));
        entries.put("gold_pine_resin", new Entry(Category.NORMAL, 800, true, 1, 11));
        entries.put("dark_pine_resin", new Entry(Category.NORMAL, 800, true, 1, 11));
        entries.put("soul_fading", new Entry(Category.NORMAL, 100, true, 2, 11));
        entries.put("soul_lost_undead", new Entry(Category.NORMAL, 400, true, 2, 11));
        entries.put("soul_lost_undead_large", new Entry(Category.NORMAL, 800, true, 2, 11));
        entries.put("soul_nameless_traveler", new Entry(Category.NORMAL, 1600, true, 2, 11));
        entries.put("soul_nameless_traveler_large", new Entry(Category.NORMAL, 2000, true, 2, 11));
        entries.put("soul_nameless_soldier", new Entry(Category.NORMAL, 4000, true, 2, 11));
        entries.put("soul_nameless_soldier_large", new Entry(Category.NORMAL, 6000, true, 2, 11));
        entries.put("soul_exhausted_warrior", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("soul_exhausted_warrior_large", new Entry(Category.NORMAL, 16000, true, 2, 11));
        entries.put("soul_crestfallen_knight", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("soul_crestfallen_knight_large", new Entry(Category.NORMAL, 40000, true, 2, 11));
        entries.put("orange_marmalade", new Entry(Category.NORMAL, 800, true, 1, 1));
        entries.put("master_key", new Entry(Category.NORMAL, 1000, true, 3, 7));
        entries.put("blackwell_blood_vial", new Entry(Category.NORMAL, 1000, true, 0, 7));
        entries.put("candy", new Entry(Category.NORMAL, 5000, true, 3, 7));
        entries.put("oil_urn", new Entry(Category.NORMAL, 600, true, 1, 1));
        entries.put("throwing_knife", new Entry(Category.NORMAL, 25, true, 1, 1));
        entries.put("undead_killer_mushroom", new Entry(Category.NORMAL, 500, true, 1, 1));
        entries.put("pure_water", new Entry(Category.NORMAL, 50, true, 1, 11));
        entries.put("stamina_tonic", new Entry(Category.NORMAL, 500, true, 0, 7));
        entries.put("snake_bone_return", new Entry(Category.IMPORTANT, 10000, false, 2, 7));
        entries.put("muddy_fish", new Entry(Category.NORMAL, 4, true, 1, 7));
        entries.put("white_sticky_thing", new Entry(Category.NORMAL, 300, true, 1, 7));
        entries.put("iron_scrap_snack", new Entry(Category.NORMAL, 300, true, 1, 7));
        entries.put("fairy_feather", new Entry(Category.NORMAL, 300, true, 1, 7));
        entries.put("golden_mead", new Entry(Category.NORMAL, 1500, true, 0, 7));
        entries.put("carpenter_nail", new Entry(Category.NORMAL, 300, true, 1, 7));
        entries.put("prescription_medicine", new Entry(Category.NORMAL, 1000, false, 2, 0));
        entries.put("girls_photo", new Entry(Category.NORMAL, 100, false, 2, 0));
        entries.put("retrieval_poker", new Entry(Category.NORMAL, 10000, true, 2, 7));
        entries.put("goat_meat", new Entry(Category.NORMAL, 8000, true, 2, 7));
        entries.put("pregnant_cake_meat", new Entry(Category.NORMAL, 8000, true, 2, 7));
        entries.put("black_ash", new Entry(Category.IMPORTANT, 0, false, 2, 11));
        entries.put("bloodstained_key", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("drink_me", new Entry(Category.IMPORTANT, 0, true, 2, 7));
        entries.put("eat_me", new Entry(Category.IMPORTANT, 0, true, 2, 7));
        entries.put("rabbit_key", new Entry(Category.NORMAL, 50000, true, 3, 7));
        entries.put("golden_egg", new Entry(Category.NORMAL, 500000, true, 2, 11));
        entries.put("train_ticket", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("entry_pass", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("queen_egg_tart", new Entry(Category.NORMAL, 8000, true, 2, 7));
        entries.put("candle_ember", new Entry(Category.NORMAL, 500, true, 0, 7));
        entries.put("roasted_cheese", new Entry(Category.NORMAL, 5000, true, 2, 7));
        entries.put("turtle_soup", new Entry(Category.NORMAL, 5000, true, 0, 7));
        entries.put("soul_black_defiled", new Entry(Category.NORMAL, 0, false, 2, 0));
        entries.put("dream_soul", new Entry(Category.NORMAL, 100000, false, 2, 0));
        entries.put("snake_god_blood", new Entry(Category.NORMAL, 2000, true, 1, 7));
        entries.put("alice", new Entry(Category.IMPORTANT, 0, false, 2, 7));
        entries.put("bills_bento", new Entry(Category.NORMAL, 500, true, 0, 7));
        entries.put("soul_outsider", new Entry(Category.NORMAL, 0, false, 2, 0));
        entries.put("soul_hero", new Entry(Category.NORMAL, 50000, true, 2, 11));
        entries.put("soul_great_hero", new Entry(Category.NORMAL, 100000, true, 2, 11));
        entries.put("match_medicine", new Entry(Category.NORMAL, 1000, true, 0, 7));
        entries.put("mad_gear", new Entry(Category.IMPORTANT, 5000, false, 2, 0));
        entries.put("nightmare_lantern", new Entry(Category.NORMAL, 1000, true, 1, 1));
        entries.put("chicken", new Entry(Category.NORMAL, 3000, true, 2, 7));
        entries.put("christmas_chicken", new Entry(Category.NORMAL, 3000, true, 2, 7));
        entries.put("mysterious_meat", new Entry(Category.NORMAL, 2, true, 2, 7));
        entries.put("satyrs_thing", new Entry(Category.NORMAL, 2, true, 2, 11));
        entries.put("mermaid_song", new Entry(Category.NORMAL, 3000, true, 0, 7));
        entries.put("ancient_kings_bone_dust", new Entry(Category.IMPORTANT, 0, true, 2, 11));
        entries.put("squirrel_fur", new Entry(Category.NORMAL, 500, true, 2, 11));
        entries.put("ice_pine_resin", new Entry(Category.NORMAL, 800, true, 1, 11));
        entries.put("scalpel", new Entry(Category.NORMAL, 25, true, 1, 1));
        entries.put("star_water", new Entry(Category.NORMAL, 10000, true, 1, 7));
        entries.put("filthy_liquid", new Entry(Category.NORMAL, 300, true, 1, 7));
        entries.put("bluebird_feather", new Entry(Category.NORMAL, 300, true, 1, 7));
        entries.put("tinker_bells_scale_powder", new Entry(Category.NORMAL, 10000, true, 1, 8));
        entries.put("ouija_board", new Entry(Category.NORMAL, 10000, true, 1, 2));
        entries.put("rolds_fountain_pen", new Entry(Category.NORMAL, 1000, true, 1, 7));
        entries.put("cursing_flower", new Entry(Category.NORMAL, 500, true, 1, 1));
        entries.put("cold_valley_breath", new Entry(Category.NORMAL, 2000, true, 1, 2));
        entries.put("helanrith_wine", new Entry(Category.NORMAL, 900000, false, 1, 11));
        entries.put("necronomicon", new Entry(Category.NORMAL, 300000, true, 1, 11));
        entries.put("book_rascal", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_fox_and_grapes", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_ugly_duckling", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_high_jumper", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_wolf_and_goats", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_hansel_and_gretel", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_sinbad", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_bremen_musicians", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_iron_hans", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_dog_of_flanders", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_little_prince", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_armored_knight", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_donkey_ears_king", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_peter_pan", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_monkey_and_crab", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_wizard_of_oz", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_match_girl", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_golden_goose", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_greedy_dog", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_pull_turnip", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_kachi_kachi_yama", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_inaba_black_rabbit", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_robin_hood", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_bluebeard", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_daddy_long_legs", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_boy_who_cried_wolf", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_winnie_the_pooh", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_pinocchio", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_nightingale", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_blank", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_snow_queen", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("book_snow_maiden", new Entry(Category.IMPORTANT, 0, true, 3, 7));
        entries.put("soul_skull_beast", new Entry(Category.IMPORTANT, 2000, true, 2, 11));
        entries.put("soul_boredom", new Entry(Category.IMPORTANT, 4000, true, 2, 11));
        entries.put("soul_pregnant_woman", new Entry(Category.IMPORTANT, 6000, true, 2, 11));
        entries.put("soul_bell_caller", new Entry(Category.IMPORTANT, 10000, true, 2, 11));
        entries.put("soul_beast_pelt", new Entry(Category.IMPORTANT, 10000, true, 2, 11));
        entries.put("soul_great_eagle", new Entry(Category.IMPORTANT, 10000, true, 2, 11));
        entries.put("soul_narcissist", new Entry(Category.IMPORTANT, 20000, true, 2, 11));
        entries.put("soul_jack", new Entry(Category.IMPORTANT, 16000, true, 2, 11));
        entries.put("soul_dorm_head", new Entry(Category.IMPORTANT, 16000, true, 2, 11));
        entries.put("soul_shining_star", new Entry(Category.IMPORTANT, 20000, true, 2, 11));
        entries.put("soul_old_knight", new Entry(Category.IMPORTANT, 6000, true, 2, 11));
        entries.put("soul_giant_house", new Entry(Category.IMPORTANT, 40000, true, 2, 11));
        entries.put("soul_knight_of_hearts", new Entry(Category.IMPORTANT, 4000, true, 2, 11));
        entries.put("soul_knight_of_spades", new Entry(Category.IMPORTANT, 4000, true, 2, 11));
        entries.put("soul_knight_of_clubs", new Entry(Category.IMPORTANT, 4000, true, 2, 11));
        entries.put("soul_slave_emperor", new Entry(Category.IMPORTANT, 40000, true, 2, 11));
        entries.put("soul_slave_queen", new Entry(Category.IMPORTANT, 40000, true, 2, 11));
        entries.put("soul_torture_queen", new Entry(Category.IMPORTANT, 20000, true, 2, 11));
        entries.put("soul_bandersnatch", new Entry(Category.IMPORTANT, 40000, true, 2, 11));
        entries.put("soul_jubjub", new Entry(Category.IMPORTANT, 40000, true, 2, 11));
        entries.put("soul_jabberwock", new Entry(Category.IMPORTANT, 40000, true, 2, 11));
        entries.put("soul_divine_fish", new Entry(Category.IMPORTANT, 100000, true, 2, 11));
        entries.put("soul_deep_sea_knight", new Entry(Category.IMPORTANT, 50000, true, 2, 11));
        entries.put("soul_evil_dragon_hunter", new Entry(Category.IMPORTANT, 50000, true, 2, 11));
        entries.put("soul_appointed_wet_nurse", new Entry(Category.IMPORTANT, 50000, true, 2, 11));
        entries.put("soul_florence", new Entry(Category.IMPORTANT, 50000, true, 2, 11));
        entries.put("soul_winter_bell_wind", new Entry(Category.IMPORTANT, 50000, true, 2, 11));
        entries.put("soul_white_unicorn", new Entry(Category.IMPORTANT, 50000, true, 2, 11));
        entries.put("soul_white_lion", new Entry(Category.IMPORTANT, 50000, true, 2, 11));
        entries.put("skill_book_soul_arrow", new Entry(Category.NORMAL, 300, true, 2, 11));
        entries.put("skill_book_soul_volley", new Entry(Category.NORMAL, 1500, true, 2, 11));
        entries.put("skill_book_soul_light", new Entry(Category.NORMAL, 300, true, 2, 11));
        entries.put("skill_book_soul_radiation", new Entry(Category.NORMAL, 500, true, 2, 11));
        entries.put("skill_book_dispel", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_see_through_attack", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_carthus_blood_curse", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_poison", new Entry(Category.NORMAL, 1000, true, 2, 11));
        entries.put("skill_book_poison_ii", new Entry(Category.NORMAL, 1500, true, 2, 11));
        entries.put("skill_book_hypnosis", new Entry(Category.NORMAL, 3000, true, 2, 11));
        entries.put("skill_book_cure", new Entry(Category.NORMAL, 1000, true, 2, 11));
        entries.put("skill_book_magic_blessing", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_rampage", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_full_blessing", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_resurrection", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_mana_absorption", new Entry(Category.NORMAL, 2000, true, 2, 11));
        entries.put("skill_book_erase", new Entry(Category.NORMAL, 2000, true, 2, 11));
        entries.put("skill_book_kings_command", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_requiem", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_grit", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_fire", new Entry(Category.NORMAL, 500, true, 2, 11));
        entries.put("skill_book_drowning_bubble", new Entry(Category.NORMAL, 500, true, 2, 11));
        entries.put("skill_book_dark_side_of_moon", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_freezing_magic_bullet", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_hellfire", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_destruction_storm", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_inner_potential", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_great_soul_arrow", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_verdant_power", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_rock_body", new Entry(Category.NORMAL, 3000, true, 2, 11));
        entries.put("skill_book_dark_orb", new Entry(Category.NORMAL, 300, true, 2, 11));
        entries.put("skill_book_dark_dance", new Entry(Category.NORMAL, 1500, true, 2, 11));
        entries.put("skill_book_dark_swarm", new Entry(Category.NORMAL, 500, true, 2, 11));
        entries.put("skill_book_divine_thunder", new Entry(Category.NORMAL, 500, true, 2, 11));
        entries.put("skill_book_divine_beast_thunder", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_meteor_swarm", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_full_curse", new Entry(Category.NORMAL, 10000, true, 2, 11));
        entries.put("skill_book_great_soul_arrow_volley", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_invisible", new Entry(Category.NORMAL, 1000, true, 2, 11));
        entries.put("skill_book_fatal_guard", new Entry(Category.NORMAL, 1000, true, 2, 11));
        entries.put("skill_book_ghost_fire", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_phalanx", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_absolute_hit", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_chaos_explosion", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_critical_strike", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_soul_shield", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_dense_spirochete", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_summon_meat_wall", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_torn_grudge", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_piercing_icicle", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_rain_of_ruin", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_gloomy_swamp", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_acid_rain", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_royal_tea", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_godspeed_dance", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_katarina_wheel", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_paladin_banner", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_black_wave", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_black_slash", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_awakening", new Entry(Category.NORMAL, 20000, true, 2, 11));
        entries.put("skill_book_serpent_embrace", new Entry(Category.NORMAL, 5000, true, 2, 11));
        entries.put("skill_book_soul_stream", new Entry(Category.NORMAL, 5000, true, 2, 11));
        return entries;
    }

    @Nullable
    public static Entry get(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null || !BlackSouls.MODID.equals(id.getNamespace())) {
            return null;
        }
        return ENTRIES.get(id.getPath());
    }

    public static void fillCreativeTab(CreativeModeTab.Output output, Category category) {
        ENTRIES.forEach((path, entry) -> {
            if (entry.category() != category) {
                return;
            }
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(BlackSouls.MODID, path));
            if (item != null) {
                output.accept(item);
            }
        });
    }
}
