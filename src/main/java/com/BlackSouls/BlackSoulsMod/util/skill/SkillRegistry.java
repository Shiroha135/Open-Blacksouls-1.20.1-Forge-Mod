package com.BlackSouls.BlackSoulsMod.util.skill;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillRegistry {
    public static final Map<String, AbstractSkill> SKILLS = new HashMap<>();

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
}
