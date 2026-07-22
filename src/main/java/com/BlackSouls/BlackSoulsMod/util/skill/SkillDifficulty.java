package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class SkillDifficulty extends AbstractSkill {
    @Override
    public String getSkillId() {
        return "bs2_skill_difficulty";
    }

    @Override
    public float getManaCost() {
        return 0.0F;
    }

    @Override
    public int getBaseCooldownTicks() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "skill.blacksouls.bs2_skill_difficulty.name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.GOLD;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/difficulty.png");
    }

    @Override
    public double getActionCost() {
        return 0.0D;
    }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
    }
}
