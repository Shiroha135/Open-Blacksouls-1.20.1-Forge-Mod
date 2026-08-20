package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

@SuppressWarnings("removal")
public class SkillGaleSixfoldThrust extends AbstractSpearSkill {

    @Override
    protected boolean isGungnirOnly() { return true; }

    @Override
    public String getSkillId() { return "bs2_skill_gale_sixfold_thrust"; }

    @Override
    public float getManaCost() { return 30.0F; }

    @Override
    public int getBaseCooldownTicks() { return 1600; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_gale_sixfold_thrust.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.GREEN; }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/gale_sixfold_thrust.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = findTargets(player, 10.0D, 3);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.gale_sixfold_thrust.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        executeVolley(player, targets, stats, true);
    }

    private void executeVolley(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats, boolean allowRepeat) {
        for (LivingEntity target : targets) {
            if (!target.isRemoved() && target.isAlive()) {
                playAnimation(target, 138);
            }
        }
        playSound(player, BlackSouls.ATTACK3_EVENT.get(), 1.0F);

        scheduleHitWave(player, targets, stats, 4, 1.5F);
        scheduleHitWave(player, targets, stats, 7, 1.4F);
        scheduleSound(player, 9, 1.3F);
        scheduleSound(player, 12, 1.2F);
        scheduleSound(player, 15, 1.1F);

        if (allowRepeat) {
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(20, () -> {
                if (player.getRandom().nextDouble() < 0.5D
                        && targets.stream().anyMatch(LivingEntity::isAlive)) {
                    executeVolley(player, targets, stats, false);
                }
            }));
        }
    }

    private void scheduleHitWave(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats, int tick, float pitch) {
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(tick, () -> {
            playSound(player, BlackSouls.SLASH10_EVENT.get(), pitch);
            for (LivingEntity target : targets) {
                applyFormulaHit(player, target, stats, 5.0D, 2.0D, 0.2D, true, false, 0.0D);
            }
        }));
    }

    private void scheduleSound(ServerPlayer player, int tick, float pitch) {
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(tick, () ->
                playSound(player, BlackSouls.SLASH10_EVENT.get(), pitch)));
    }
}
