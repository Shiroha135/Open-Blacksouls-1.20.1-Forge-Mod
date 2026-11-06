package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.story.OriginalDodoRunService;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class SkillDodoRun extends AbstractSkill {
    public static final String ID = "bs2_skill_dodo_run";

    @Override public String getSkillId() { return ID; }
    @Override public float getManaCost() { return 0.0F; }
    @Override public int getBaseCooldownTicks() { return 0; }
    @Override public double getActionCost() { return 0.0D; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_dodo_run.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.YELLOW; }
    @Override public ResourceLocation getIcon() { return ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID, "textures/gui/skills/dodo_run.png"); }
    @Override public boolean isUnlockedForGUI(Player player) { return SkillUtils.hasLearnedSkill(player, ID); }
    @Override public boolean isUsableInTurnBattle() { return false; }
    @Override public boolean canCast(ServerPlayer player, BSPlayerStats stats) { return OriginalDodoRunService.canOpen(player); }
    @Override public boolean canCastInTurnBattle(ServerPlayer player, BSPlayerStats stats) { return false; }
    @Override public boolean canAutoCast(ServerPlayer player, BSPlayerStats stats) { return false; }
    @Override public void consumeAndSetCooldown(ServerPlayer player, BSPlayerStats stats) { }
    @Override public void execute(ServerPlayer player, BSPlayerStats stats) { OriginalDodoRunService.open(player); }
}
