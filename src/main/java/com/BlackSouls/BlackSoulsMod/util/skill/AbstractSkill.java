package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public abstract class AbstractSkill {

    public abstract String getSkillId();
    public abstract float getManaCost();
    public abstract int getBaseCooldownTicks();
    public abstract String getTranslationKey();
    public abstract ChatFormatting getTextColor();

    /**
     * GUI 图标路径。附属 mod 可 override 返回自己 namespace 下的贴图。
     * 默认返回 BS 自带占位图标。
     */
    public ResourceLocation getIcon() {
        return new ResourceLocation("blacksouls", "textures/gui/skills/reinforce.png");
    }

    public double getActionCost() {
        return SkillUtils.DEFAULT_SKILL_ACTION_COST;
    }

    public int getTurnCooldownRounds() {
        return Math.max(0, (int) Math.ceil(getBaseCooldownTicks() / 200.0D));
    }

    public abstract boolean isUnlockedForGUI(Player player);

    protected float getEffectiveManaCost(BSPlayerStats stats) {
        double rate = stats == null ? 1.0D : stats.mpCostRate;
        return (float) Math.max(0.0D, getManaCost() * rate);
    }

    public boolean canCast(ServerPlayer player, BSPlayerStats stats) {
        if (!canCastInTurnBattle(player, stats)) {
            return false;
        }

        if (SkillUtils.hasInfiniteCooldownAccessory(player)) {
            return true;
        }
        if (SkillUtils.isChronoRewindActive(player) && !"bs2_skill_chrono_clock".equals(getSkillId())) {
            return true;
        }

        long currentTime = player.level().getGameTime();
        long lastTime = SkillUtils.getPersistedData(player).getLong(SkillUtils.getCooldownTag(getSkillId()));
        long realCooldown = getBaseCooldownTicks();

        if (currentTime - lastTime < realCooldown) {
            double timeLeft = (realCooldown - (currentTime - lastTime)) / 20.0;
            player.sendSystemMessage(Component.literal(String.format("冷却中: %.1fs", timeLeft)).withStyle(ChatFormatting.RED));
            return false;
        }

        return true;
    }

    public boolean canCastInTurnBattle(ServerPlayer player, BSPlayerStats stats) {
        if (!SkillUtils.shouldBypassManaCost(player) && stats.mp < getEffectiveManaCost(stats)) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_mp").withStyle(ChatFormatting.RED));
            return false;
        }

        if (!SkillUtils.hasEnoughActionPoints(player, getActionCost())) {
            player.sendSystemMessage(Component.literal(String.format(
                    "行动值不足！(%.2f/%.2f，需要%.2f)",
                    SkillUtils.getCurrentActionPoints(player),
                    SkillUtils.getMaxActionPoints(player),
                    getActionCost()
            )).withStyle(ChatFormatting.GREEN));
            return false;
        }
        return true;
    }

    public boolean canAutoCast(ServerPlayer player, BSPlayerStats stats) {
        if (player == null || stats == null || !isUnlockedForGUI(player)) {
            return false;
        }
        if (!SkillUtils.shouldBypassManaCost(player) && stats.mp < getEffectiveManaCost(stats)) {
            return false;
        }
        if (!SkillUtils.hasEnoughActionPoints(player, getActionCost())) {
            return false;
        }
        if (SkillUtils.hasInfiniteCooldownAccessory(player)
                || (SkillUtils.isChronoRewindActive(player) && !"bs2_skill_chrono_clock".equals(getSkillId()))) {
            return true;
        }
        long currentTime = player.level().getGameTime();
        long lastTime = SkillUtils.getPersistedData(player).getLong(SkillUtils.getCooldownTag(getSkillId()));
        return currentTime - lastTime >= getBaseCooldownTicks();
    }

    public void consumeAndSetCooldown(ServerPlayer player, BSPlayerStats stats) {
        SkillUtils.consumeMana(player, getEffectiveManaCost(stats));
        SkillUtils.consumeActionPoints(player, getActionCost());
        if (!SkillUtils.hasInfiniteCooldownAccessory(player)
                && !(SkillUtils.isChronoRewindActive(player) && !"bs2_skill_chrono_clock".equals(getSkillId()))) {
            SkillUtils.getPersistedData(player).putLong(SkillUtils.getCooldownTag(getSkillId()), player.level().getGameTime());
        }
        StatEventHandler.syncToClient(player);
    }

    public void consumeForTurnBattle(ServerPlayer player, BSPlayerStats stats) {
        SkillUtils.consumeMana(player, getEffectiveManaCost(stats));
        SkillUtils.consumeActionPoints(player, getActionCost());
        StatEventHandler.syncToClient(player);
    }

    public abstract void execute(ServerPlayer player, BSPlayerStats stats);
}
