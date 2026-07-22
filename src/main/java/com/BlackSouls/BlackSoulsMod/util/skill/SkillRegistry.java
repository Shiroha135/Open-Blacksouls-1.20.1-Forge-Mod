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
        register(new SkillInvisibleBody());
        register(new SkillRequiem());
        register(new SkillGrit());
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
        register(new SkillSoulArrow());
        register(new SkillSoulLight());
        register(new SkillSoulRadiation());
        register(new SkillCarthusBloodCurse());
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
