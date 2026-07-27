package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.item.weapon.ItemOriginalBow;
import com.BlackSouls.BlackSoulsMod.item.weapon.ItemOriginalWeapon;
import com.BlackSouls.BlackSoulsMod.util.skill.AbstractSkill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TurnBattleVfxResolver {
    public static Cue resolveWeapon(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new Cue(1, Target.ENEMY);
        }
        Item item = stack.getItem();
        if (item instanceof ItemOriginalWeapon originalWeapon) {
            return new Cue(originalWeapon.getTurnBattleAnimationId(player), Target.ENEMY);
        }
        if (item instanceof ItemOriginalBow originalBow) {
            return new Cue(originalBow.getTurnBattleAnimationId(), Target.ENEMY);
        }
        int animationId = switch (item.getClass().getSimpleName()) {
            case "ItemKnightSword", "ItemKnightKingSword" -> 125;
            case "ItemGreatSword", "ItemGiantSword" -> 127;
            case "ItemBroadSpear", "ItemGungnir" -> 128;
            case "ItemAndorSword" -> 132;
            case "ItemThiefsDagger", "ItemGreatThiefsDagger" -> 150;
            case "ItemDrakeSword" -> 169;
            case "ItemClub", "ItemKingClub" -> 212;
            case "ItemVorpalBlade", "ItemVorpalSword" -> 242;
            case "ItemBraveSwordVorpal" -> 249;
            case "ItemMurderersShotgun" -> 246;
            case "ItemBandersnatchSword" -> 314;
            default -> 7;
        };
        return new Cue(animationId, Target.ENEMY);
    }

    public static Cue resolveSkill(AbstractSkill skill) {
        if (skill == null) {
            return Cue.NONE;
        }
        return switch (skill.getSkillId()) {
            case "bs2_skill_absolute_hit" -> new Cue(33, Target.PLAYER);
            case "bs2_skill_acid_rain" -> new Cue(514, Target.ENEMY);
            case "bs2_skill_aim" -> new Cue(95, Target.PLAYER);
            case "bs2_skill_armor_break" -> new Cue(135, Target.ENEMY);
            case "bs2_skill_armor_pierce" -> new Cue(137, Target.ENEMY);
            case "bs2_skill_arrow_rain" -> new Cue(225, Target.ENEMY);
            case "bs2_skill_aura_blade" -> new Cue(136, Target.ENEMY);
            case "bs2_skill_awakening" -> new Cue(81, Target.PLAYER);
            case "bs2_skill_berserker_roar" -> new Cue(34, Target.PLAYER);
            case "bs2_skill_black_slash" -> new Cue(192, Target.ENEMY);
            case "bs2_skill_black_wave" -> new Cue(173, Target.ENEMY);
            case "bs2_skill_blessing_of_pain" -> new Cue(384, Target.ENEMY);
            case "bs2_skill_blood_edge" -> new Cue(234, Target.ENEMY);
            case "bs2_skill_blood_trail" -> new Cue(95, Target.PLAYER);
            case "bs2_skill_blood_trial" -> new Cue(384, Target.ENEMY);
            case "bs2_skill_bullet_load" -> new Cue(492, Target.PLAYER);
            case "bs2_skill_carthus_blood_curse" -> new Cue(56, Target.PLAYER);
            case "bs2_skill_chakra" -> new Cue(38, Target.PLAYER);
            case "bs2_skill_chaos_explosion" -> new Cue(394, Target.ENEMY);
            case "bs2_skill_cleave_in_two" -> new Cue(522, Target.ENEMY);
            case "bs2_skill_corpse_dragon_awe" -> new Cue(320, Target.ENEMY);
            case "bs2_skill_counter" -> new Cue(0, Target.PLAYER);
            case "bs2_skill_cross_slash" -> new Cue(518, Target.ENEMY);
            case "bs2_skill_crushing_water" -> new Cue(356, Target.ENEMY);
            case "bs2_skill_cure" -> new Cue(40, Target.PLAYER);
            case "bs2_skill_dark_dance" -> new Cue(296, Target.ENEMY);
            case "bs2_skill_dark_orb" -> new Cue(253, Target.ENEMY);
            case "bs2_skill_dark_side_of_moon" -> new Cue(49, Target.ENEMY);
            case "bs2_skill_dark_swarm" -> new Cue(265, Target.ENEMY);
            case "bs2_skill_darkness" -> new Cue(78, Target.ENEMY);
            case "bs2_skill_dead_strike" -> new Cue(181, Target.ENEMY);
            case "bs2_skill_decapitation" -> new Cue(142, Target.ENEMY);
            case "bs2_skill_delicious_turtle_soup" -> new Cue(37, Target.PLAYER);
            case "bs2_skill_dense_spirochete" -> new Cue(499, Target.ENEMY);
            case "bs2_skill_destruction_storm" -> new Cue(103, Target.ENEMY);
            case "bs2_skill_dispel" -> new Cue(46, Target.ENEMY);
            case "bs2_skill_divine_beast_thunder" -> new Cue(100, Target.ENEMY);
            case "bs2_skill_divine_thunder" -> new Cue(65, Target.ENEMY);
            case "bs2_skill_double_collision" -> new Cue(129, Target.ENEMY);
            case "bs2_skill_dragon_shockwave" -> new Cue(175, Target.ENEMY);
            case "bs2_skill_drowning_bubble" -> new Cue(69, Target.ENEMY);
            case "bs2_skill_eclipse" -> new Cue(376, Target.PLAYER);
            case "bs2_skill_erase" -> new Cue(41, Target.PLAYER);
            case "bs2_skill_fatal_guard" -> new Cue(81, Target.PLAYER);
            case "bs2_skill_fire" -> new Cue(58, Target.ENEMY);
            case "bs2_skill_flesh_carve" -> new Cue(233, Target.ENEMY);
            case "bs2_skill_foot_shot" -> new Cue(226, Target.ENEMY);
            case "bs2_skill_forward_slash" -> new Cue(338, Target.ENEMY);
            case "bs2_skill_freezing_magic_bullet" -> new Cue(61, Target.ENEMY);
            case "bs2_skill_full_blessing" -> new Cue(44, Target.PLAYER);
            case "bs2_skill_full_curse" -> new Cue(46, Target.ENEMY);
            case "bs2_skill_gale_sixfold_thrust" -> new Cue(138, Target.ENEMY);
            case "bs2_skill_ghost_fire" -> new Cue(266, Target.ENEMY);
            case "bs2_skill_giant_guillotine" -> new Cue(133, Target.ENEMY);
            case "bs2_skill_gloomy_swamp" -> new Cue(513, Target.ENEMY);
            case "bs2_skill_godspeed_dance" -> new Cue(354, Target.PLAYER);
            case "bs2_skill_great_soul_arrow" -> new Cue(124, Target.ENEMY);
            case "bs2_skill_great_soul_arrow_volley" -> new Cue(124, Target.ENEMY);
            case "bs2_skill_green_collapse" -> new Cue(381, Target.ENEMY);
            case "bs2_skill_gunpowder_replenish" -> new Cue(111, Target.PLAYER);
            case "bs2_skill_haki" -> new Cue(95, Target.PLAYER);
            case "bs2_skill_hasso" -> new Cue(43, Target.PLAYER);
            case "bs2_skill_heaven_shattering_thunder" -> new Cue(553, Target.ENEMY);
            case "bs2_skill_heavy_strike" -> new Cue(315, Target.ENEMY);
            case "bs2_skill_hellfire" -> new Cue(98, Target.ENEMY);
            case "bs2_skill_hellfire_blade" -> new Cue(179, Target.ENEMY);
            case "bs2_skill_hide_and_seek" -> new Cue(395, Target.ENEMY);
            case "bs2_skill_hundred_fists" -> new Cue(241, Target.ENEMY);
            case "bs2_skill_hypnosis" -> new Cue(54, Target.ENEMY);
            case "bs2_skill_iai" -> new Cue(337, Target.ENEMY);
            case "bs2_skill_inner_potential" -> new Cue(466, Target.PLAYER);
            case "bs2_skill_iron_hammer" -> new Cue(143, Target.ENEMY);
            case "bs2_skill_issen" -> new Cue(144, Target.ENEMY);
            case "bs2_skill_juggling_evasion" -> new Cue(0, Target.PLAYER);
            case "bs2_skill_katarina_wheel" -> new Cue(126, Target.ENEMY);
            case "bs2_skill_kings_command" -> new Cue(43, Target.PLAYER);
            case "bs2_skill_knights_glory" -> new Cue(33, Target.PLAYER);
            case "bs2_skill_lake_god_apocalypse" -> new Cue(380, Target.ENEMY);
            case "bs2_skill_lion_whirlwind" -> new Cue(552, Target.ENEMY);
            case "bs2_skill_mad_bird_call" -> new Cue(282, Target.PLAYER);
            case "bs2_skill_magic_blessing" -> new Cue(43, Target.PLAYER);
            case "bs2_skill_mana_absorption" -> new Cue(49, Target.ENEMY);
            case "bs2_skill_mana_burn" -> new Cue(229, Target.ENEMY);
            case "bs2_skill_mana_recovery" -> new Cue(228, Target.PLAYER);
            case "bs2_skill_massacre_axe" -> new Cue(134, Target.ENEMY);
            case "bs2_skill_mental_break" -> new Cue(139, Target.ENEMY);
            case "bs2_skill_mental_focus" -> new Cue(350, Target.PLAYER);
            case "bs2_skill_meteor_swarm" -> new Cue(106, Target.ENEMY);
            case "bs2_skill_mind_eye" -> new Cue(33, Target.PLAYER);
            case "bs2_skill_overhead_barrage" -> new Cue(236, Target.ENEMY);
            case "bs2_skill_paladin_banner" -> new Cue(95, Target.PLAYER);
            case "bs2_skill_peerless_challenge" -> new Cue(33, Target.ENEMY);
            case "bs2_skill_phalanx" -> new Cue(162, Target.PLAYER);
            case "bs2_skill_piercing_icicle" -> new Cue(511, Target.ENEMY);
            case "bs2_skill_poison" -> new Cue(299, Target.ENEMY);
            case "bs2_skill_poison_ii" -> new Cue(300, Target.ENEMY);
            case "bs2_skill_pommel_stun" -> new Cue(521, Target.ENEMY);
            case "bs2_skill_quick_reload" -> new Cue(111, Target.PLAYER);
            case "bs2_skill_radiant_blade" -> new Cue(87, Target.PLAYER);
            case "bs2_skill_rage" -> new Cue(358, Target.PLAYER);
            case "bs2_skill_rain_of_ruin" -> new Cue(512, Target.ENEMY);
            case "bs2_skill_rampage" -> new Cue(5, Target.ENEMY);
            case "bs2_skill_reckless_strike" -> new Cue(5, Target.ENEMY);
            case "bs2_skill_reinforce" -> new Cue(335, Target.PLAYER);
            case "bs2_skill_resurrection" -> new Cue(42, Target.PLAYER);
            case "bs2_skill_rock_body" -> new Cue(288, Target.PLAYER);
            case "bs2_skill_royal_tea" -> new Cue(44, Target.PLAYER);
            case "bs2_skill_self_harm" -> new Cue(180, Target.PLAYER);
            case "bs2_skill_serpent_embrace" -> new Cue(531, Target.ENEMY);
            case "bs2_skill_shadowless" -> new Cue(31, Target.PLAYER);
            case "bs2_skill_shield_slam" -> new Cue(161, Target.ENEMY);
            case "bs2_skill_shotgun_blast" -> new Cue(247, Target.ENEMY);
            case "bs2_skill_silver_moon_thunder_axe" -> new Cue(231, Target.ENEMY);
            case "bs2_skill_sin_burst" -> new Cue(168, Target.ENEMY);
            case "bs2_skill_sin_crush" -> new Cue(167, Target.ENEMY);
            case "bs2_skill_sky_cleaving_slash" -> new Cue(548, Target.ENEMY);
            case "bs2_skill_slash_down" -> new Cue(130, Target.ENEMY);
            case "bs2_skill_slaughter_begins" -> new Cue(34, Target.PLAYER);
            case "bs2_skill_smoldering_frenzy" -> new Cue(316, Target.PLAYER);
            case "bs2_skill_solar_flare" -> new Cue(378, Target.ENEMY);
            case "bs2_skill_soul_arrow" -> new Cue(75, Target.ENEMY);
            case "bs2_skill_soul_collapse" -> new Cue(321, Target.ENEMY);
            case "bs2_skill_soul_harvest" -> new Cue(221, Target.ENEMY);
            case "bs2_skill_soul_light" -> new Cue(37, Target.ENEMY);
            case "bs2_skill_soul_radiation" -> new Cue(76, Target.ENEMY);
            case "bs2_skill_soul_shield" -> new Cue(396, Target.PLAYER);
            case "bs2_skill_soul_stream" -> new Cue(534, Target.ENEMY);
            case "bs2_skill_soul_volley" -> new Cue(295, Target.ENEMY);
            case "bs2_skill_strong_crush" -> new Cue(240, Target.ENEMY);
            case "bs2_skill_struggle" -> new Cue(95, Target.PLAYER);
            case "bs2_skill_summon_meat_wall" -> new Cue(118, Target.PLAYER);
            case "bs2_skill_sweep" -> new Cue(127, Target.ENEMY);
            case "bs2_skill_tempest_rend" -> new Cue(339, Target.ENEMY);
            case "bs2_skill_torn_grudge" -> new Cue(510, Target.ENEMY);
            case "bs2_skill_triple_shot" -> new Cue(227, Target.ENEMY);
            case "bs2_skill_true_soul_harvest" -> new Cue(221, Target.ENEMY);
            case "bs2_skill_tsubame_gaeshi" -> new Cue(141, Target.ENEMY);
            case "bs2_skill_twilight_of_grudge" -> new Cue(319, Target.ENEMY);
            case "bs2_skill_ultimate_triple_slash" -> new Cue(371, Target.ENEMY);
            case "bs2_skill_verdant_power" -> new Cue(38, Target.PLAYER);
            case "bs2_skill_visceral_attack" -> new Cue(423, Target.ENEMY);
            case "bs2_skill_vorpal_slash" -> new Cue(243, Target.ENEMY);
            case "bs2_skill_weapon_break" -> new Cue(135, Target.ENEMY);
            case "bs2_skill_wrath_of_twilight" -> new Cue(523, Target.ENEMY);
            case "bs2_skill_zenith_blade" -> new Cue(377, Target.ENEMY);
            default -> Cue.NONE;
        };
    }

    public enum Target {
        ENEMY,
        PLAYER
    }

    public record Cue(int animationId, Target target) {
        public static final Cue NONE = new Cue(0, Target.ENEMY);

        public boolean valid() {
            return animationId > 0;
        }
    }

    private TurnBattleVfxResolver() {
    }
}
