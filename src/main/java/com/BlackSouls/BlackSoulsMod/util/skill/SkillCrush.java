package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class SkillCrush extends AbstractSkill {
    @Override
    public String getSkillId() {
        return "bs2_skill_crush";
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
        return "skill.blacksouls.bs2_skill_crush.name";
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.GOLD;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/crush.png");
    }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public boolean canCast(ServerPlayer player, BSPlayerStats stats) {
        return false;
    }

    @Override
    public boolean isTurnBattleNonDamage() {
        return true;
    }

    @Override
    public boolean requiresTurnBattleTarget() {
        return true;
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
    }

    @Override
    public void executeInTurnBattle(ServerPlayer player, BSPlayerStats stats, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        target.level().playSound(null, target.blockPosition(), BlackSouls.CRASH_EVENT.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable(
                "message.blacksouls.skill.crush.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
    }
}
