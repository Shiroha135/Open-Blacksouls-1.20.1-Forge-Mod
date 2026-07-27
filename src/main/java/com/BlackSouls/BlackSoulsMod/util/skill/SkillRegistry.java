package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkillRegistry {
    public static final Map<String, AbstractSkill> SKILLS = new LinkedHashMap<>();
    private static final String PUPPET_SKILL_CURSOR = "bs2_puppet_skill_cursor";
    private static final Set<String> PUPPET_SKILLS = Set.of(
            "bs2_skill_slash_down", "bs2_skill_massacre_axe", "bs2_skill_giant_guillotine",
            "bs2_skill_pommel_stun", "bs2_skill_cleave_in_two", "bs2_skill_wrath_of_twilight",
            "bs2_skill_foot_shot", "bs2_skill_triple_shot", "bs2_skill_arrow_rain",
            "bs2_skill_mana_burn", "bs2_skill_mental_break", "bs2_skill_silver_moon_thunder_axe",
            "bs2_skill_flesh_carve", "bs2_skill_blood_trail", "bs2_skill_blood_edge",
            "bs2_skill_shield_slam", "bs2_skill_darkness", "bs2_skill_sin_crush", "bs2_skill_sin_burst",
            "bs2_skill_dead_strike", "bs2_skill_overhead_barrage", "bs2_skill_hundred_fists",
            "bs2_skill_iai", "bs2_skill_forward_slash", "bs2_skill_tempest_rend",
            "bs2_skill_reckless_strike", "bs2_skill_double_collision",
            "bs2_skill_soul_harvest", "bs2_skill_true_soul_harvest",
            "bs2_skill_storm_king", "bs2_skill_storm_overlord",
            "bs2_skill_moonlight_blade", "bs2_skill_moonlight_break",
            "bs2_skill_twilight_of_grudge", "bs2_skill_soul_collapse",
            "bs2_skill_crushing_water", "bs2_skill_zenith_blade", "bs2_skill_solar_flare",
            "bs2_skill_lake_god_apocalypse", "bs2_skill_green_collapse",
            "bs2_skill_cross_slash", "bs2_skill_visceral_attack", "bs2_skill_sky_cleaving_slash",
            "bs2_skill_heaven_shattering_thunder", "bs2_skill_lion_whirlwind",
            "bs2_skill_strong_crush"
    );

    public static void init() {
        register(new SkillSoulArrow());
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.SOUL_VOLLEY));
        register(new SkillSoulLight());
        register(new SkillSoulRadiation());
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DISPEL));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.SEE_THROUGH_ATTACK));
        register(new SkillCarthusBloodCurse());
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.POISON));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.POISON_II));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.HYPNOSIS));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.CURE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.MAGIC_BLESSING));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.RAMPAGE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.FULL_BLESSING));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.RESURRECTION));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.MANA_ABSORPTION));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.ERASE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.KINGS_COMMAND));
        register(new SkillRequiem());
        register(new SkillGrit());
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.FIRE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DROWNING_BUBBLE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DARK_SIDE_OF_MOON));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.FREEZING_MAGIC_BULLET));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.HELLFIRE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DESTRUCTION_STORM));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.INNER_POTENTIAL));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.GREAT_SOUL_ARROW));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.VERDANT_POWER));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.ROCK_BODY));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DARK_ORB));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DARK_DANCE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DARK_SWARM));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DIVINE_THUNDER));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DIVINE_BEAST_THUNDER));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.METEOR_SWARM));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.FULL_CURSE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.GREAT_SOUL_ARROW_VOLLEY));
        register(new SkillInvisibleBody());
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.FATAL_GUARD));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.GHOST_FIRE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.PHALANX));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.ABSOLUTE_HIT));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.CHAOS_EXPLOSION));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.CRITICAL_STRIKE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.SOUL_SHIELD));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.DENSE_SPIROCHETE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.SUMMON_MEAT_WALL));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.TORN_GRUDGE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.PIERCING_ICICLE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.RAIN_OF_RUIN));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.GLOOMY_SWAMP));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.ACID_RAIN));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.ROYAL_TEA));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.GODSPEED_DANCE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.KATARINA_WHEEL));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.PALADIN_BANNER));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.BLACK_WAVE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.BLACK_SLASH));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.AWAKENING));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.SERPENT_EMBRACE));
        register(new SkillOriginalMagic(SkillOriginalMagic.Profile.SOUL_STREAM));
        register(new SkillVorpalSlash());
        register(new SkillWeaponBreak());
        register(new SkillArmorBreak());
        register(new SkillAuraBlade());
        register(new SkillDragonShockwave());
        register(new SkillShotgunBreak());
        register(new SkillKnightsGlory());
        register(new SkillRadiantBlade());
        register(new SkillHellfireBlade());
        register(new SkillUltimateTripleSlash());
        register(new SkillReinforce());
        register(new SkillDecapitation());
        register(new SkillHideAndSeek());
        register(new SkillShadowless());
        register(new SkillSweep());
        register(new SkillBerserkerRoar());
        register(new SkillIronHammer());
        register(new SkillArmorPierce());
        register(new SkillGaleSixfoldThrust());
        register(new SkillStruggle());
        register(new SkillHeavyStrike());
        register(new SkillSmolderingFrenzy());
        register(new SkillHasso());
        register(new SkillTsubameGaeshi());
        register(new SkillIssen());
        register(new SkillCleaverAxeArt(SkillCleaverAxeArt.Art.SLASH_DOWN));
        register(new SkillCleaverAxeArt(SkillCleaverAxeArt.Art.MASSACRE_AXE));
        register(new SkillCleaverAxeArt(SkillCleaverAxeArt.Art.GIANT_GUILLOTINE));
        register(new SkillRagnarokArt(SkillRagnarokArt.Art.POMMEL_STUN));
        register(new SkillRagnarokArt(SkillRagnarokArt.Art.CLEAVE_IN_TWO));
        register(new SkillRagnarokArt(SkillRagnarokArt.Art.WRATH_OF_TWILIGHT));
        register(new SkillBowArt(SkillBowArt.Art.FOOT_SHOT));
        register(new SkillBowArt(SkillBowArt.Art.TRIPLE_SHOT));
        register(new SkillBowArt(SkillBowArt.Art.ARROW_RAIN));
        register(new SkillMaceArt(SkillMaceArt.Art.MANA_RECOVERY));
        register(new SkillMaceArt(SkillMaceArt.Art.MANA_BURN));
        register(new SkillHalberdArt(SkillHalberdArt.Art.COUNTER));
        register(new SkillHalberdArt(SkillHalberdArt.Art.MENTAL_BREAK));
        register(new SkillHalberdArt(SkillHalberdArt.Art.SILVER_MOON_THUNDER_AXE));
        register(new SkillBeastSawArt(SkillBeastSawArt.Art.FLESH_CARVE));
        register(new SkillBeastSawArt(SkillBeastSawArt.Art.BLOOD_TRAIL));
        register(new SkillBeastSawArt(SkillBeastSawArt.Art.BLOOD_EDGE));
        register(new SkillFortressArt());
        register(new SkillDarkSwordArt(SkillDarkSwordArt.Art.DARKNESS));
        register(new SkillDarkSwordArt(SkillDarkSwordArt.Art.SIN_CRUSH));
        register(new SkillDarkSwordArt(SkillDarkSwordArt.Art.SIN_BURST));
        register(new SkillBrokenSwordArt(SkillBrokenSwordArt.Art.SELF_HARM));
        register(new SkillBrokenSwordArt(SkillBrokenSwordArt.Art.DEAD_STRIKE));
        register(new SkillWarhammerArt(SkillWarhammerArt.Art.AIM));
        register(new SkillWarhammerArt(SkillWarhammerArt.Art.OVERHEAD_BARRAGE));
        register(new SkillFistArt(SkillFistArt.Art.HAKI));
        register(new SkillFistArt(SkillFistArt.Art.HUNDRED_FISTS));
        register(new SkillKatanaArt(SkillKatanaArt.Art.IAI));
        register(new SkillKatanaArt(SkillKatanaArt.Art.FORWARD_SLASH));
        register(new SkillKatanaArt(SkillKatanaArt.Art.TEMPEST_REND));
        register(new SkillIronBallArt(SkillIronBallArt.Art.CHAKRA));
        register(new SkillIronBallArt(SkillIronBallArt.Art.RECKLESS_STRIKE));
        register(new SkillIronBallArt(SkillIronBallArt.Art.DOUBLE_COLLISION));
        register(new SkillHansGunArt(SkillHansGunArt.Art.GUNPOWDER_REPLENISH));
        register(new SkillHansGunArt(SkillHansGunArt.Art.QUICK_RELOAD));
        register(new SkillJudgmentScytheArt(SkillJudgmentScytheArt.Art.SOUL_HARVEST));
        register(new SkillJudgmentScytheArt(SkillJudgmentScytheArt.Art.TRUE_SOUL_HARVEST));
        register(new SkillStormRulerArt(SkillStormRulerArt.Art.STORM_KING));
        register(new SkillStormRulerArt(SkillStormRulerArt.Art.STORM_OVERLORD));
        register(new SkillMoonlightGreatswordArt(SkillMoonlightGreatswordArt.Art.MOONLIGHT_BLADE));
        register(new SkillMoonlightGreatswordArt(SkillMoonlightGreatswordArt.Art.MOONLIGHT_BREAK));
        register(new SkillCorruptScytheArt(SkillCorruptScytheArt.Art.TWILIGHT_OF_GRUDGE));
        register(new SkillCorruptScytheArt(SkillCorruptScytheArt.Art.CORPSE_DRAGON_AWE));
        register(new SkillCorruptScytheArt(SkillCorruptScytheArt.Art.SOUL_COLLAPSE));
        register(new SkillMadBowArt());
        register(new SkillRlyehStaffArt());
        register(new SkillDeepSeaAnchorArt(SkillDeepSeaAnchorArt.Art.CRUSHING_WATER));
        register(new SkillDeepSeaAnchorArt(SkillDeepSeaAnchorArt.Art.RAGE));
        register(new SkillLostSwordArt(SkillLostSwordArt.Art.ECLIPSE));
        register(new SkillLostSwordArt(SkillLostSwordArt.Art.ZENITH_BLADE));
        register(new SkillLostSwordArt(SkillLostSwordArt.Art.SOLAR_FLARE));
        register(new SkillGlachidArt(SkillGlachidArt.Art.LAKE_GOD_APOCALYPSE));
        register(new SkillGlachidArt(SkillGlachidArt.Art.GREEN_COLLAPSE));
        register(new SkillSlaughtererChainsawArt(SkillSlaughtererChainsawArt.Art.BLOOD_TRIAL));
        register(new SkillSlaughtererChainsawArt(SkillSlaughtererChainsawArt.Art.BLESSING_OF_PAIN));
        register(new SkillSlaughtererChainsawArt(SkillSlaughtererChainsawArt.Art.SLAUGHTER_BEGINS));
        register(new SkillSpoonArt());
        register(new SkillHolyGunbladeArt(SkillHolyGunbladeArt.Art.CROSS_SLASH));
        register(new SkillHolyGunbladeArt(SkillHolyGunbladeArt.Art.BULLET_LOAD));
        register(new SkillHolyGunbladeArt(SkillHolyGunbladeArt.Art.VISCERAL_ATTACK));
        register(new SkillEuniceRapierArt(SkillEuniceRapierArt.Art.SKY_CLEAVING_SLASH));
        register(new SkillEuniceRapierArt(SkillEuniceRapierArt.Art.MIND_EYE));
        register(new SkillEuniceRapierArt(SkillEuniceRapierArt.Art.PEERLESS_CHALLENGE));
        register(new SkillRaidenAxesArt(SkillRaidenAxesArt.Art.HEAVEN_SHATTERING_THUNDER));
        register(new SkillRaidenAxesArt(SkillRaidenAxesArt.Art.LION_WHIRLWIND));
        register(new SkillRingArt(SkillRingArt.Art.STRONG_CRUSH));
        register(new SkillRingArt(SkillRingArt.Art.JUGGLING_EVASION));
    }

    public static void register(AbstractSkill skill) {
        SKILLS.put(skill.getSkillId(), skill);
    }

    public static Set<String> getSkillBookSkillIds() {
        java.util.LinkedHashSet<String> skillIds = new java.util.LinkedHashSet<>();
        skillIds.add("bs2_skill_grit");
        skillIds.add("bs2_skill_invisible_body");
        skillIds.add("bs2_skill_requiem");
        skillIds.add("bs2_skill_soul_arrow");
        skillIds.add("bs2_skill_soul_light");
        skillIds.add("bs2_skill_soul_radiation");
        skillIds.add("bs2_skill_carthus_blood_curse");
        for (SkillOriginalMagic.Profile profile : SkillOriginalMagic.Profile.values()) {
            skillIds.add(profile.getSkillId());
        }
        return java.util.Collections.unmodifiableSet(skillIds);
    }

    public static List<AbstractSkill> getAvailableSkills(Player player) {
        List<AbstractSkill> available = new ArrayList<>();
        for (AbstractSkill skill : SKILLS.values()) {
            if (skill.isUnlockedForGUI(player)) {
                available.add(skill);
            }
        }
        return available;
    }

    public static boolean tryCastPuppetSkill(ServerPlayer player, BSPlayerStats stats, LivingEntity target) {
        if (player == null || stats == null || target == null
                || (BlackSouls.BUFF_STUN.isPresent() && player.hasEffect(BlackSouls.BUFF_STUN.get()))
                || (BlackSouls.BUFF_SILENCE.isPresent() && player.hasEffect(BlackSouls.BUFF_SILENCE.get()))
                || (BlackSouls.BUFF_BERSERK.isPresent() && player.hasEffect(BlackSouls.BUFF_BERSERK.get()))) {
            return false;
        }

        String[] bindings = {stats.skillZ, stats.skillX, stats.skillC, stats.skillV};
        net.minecraft.nbt.CompoundTag data = SkillUtils.getPersistedData(player);
        int start = Math.floorMod(data.getInt(PUPPET_SKILL_CURSOR), bindings.length);
        for (int offset = 0; offset < bindings.length; offset++) {
            int index = (start + offset) % bindings.length;
            String skillId = bindings[index];
            AbstractSkill skill = SKILLS.get(skillId);
            if (!(skill instanceof AbstractWeaponCombatSkill)
                    || !PUPPET_SKILLS.contains(skillId)
                    || !skill.canAutoCast(player, stats)) {
                continue;
            }

            data.putInt(PUPPET_SKILL_CURSOR, (index + 1) % bindings.length);
            AbstractWeaponCombatSkill.setPuppetTarget(target);
            try {
                skill.consumeAndSetCooldown(player, stats);
                skill.execute(player, stats);
            } finally {
                AbstractWeaponCombatSkill.clearPuppetTarget();
            }
            return true;
        }
        return false;
    }
}
