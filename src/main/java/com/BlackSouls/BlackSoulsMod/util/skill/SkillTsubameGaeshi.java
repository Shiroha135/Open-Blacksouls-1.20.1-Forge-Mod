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

public class SkillTsubameGaeshi extends AbstractOriginalWeaponSkill {
    public SkillTsubameGaeshi() {
        super(Family.MAGIC_BLADE, false);
    }

    @Override public String getSkillId() { return "bs2_skill_tsubame_gaeshi"; }
    @Override public float getManaCost() { return 10.0F; }
    @Override public int getBaseCooldownTicks() { return 1000; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_tsubame_gaeshi.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_RED; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/tsubame_gaeshi.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = findTargets(player, 10.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.tsubame_gaeshi.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        targets.forEach(target -> playAnimation(target, 141));
        playSound(player, BlackSouls.SLASH4_EVENT.get(), 0.8F);
        playSound(player, BlackSouls.DOWN2_EVENT.get(), 1.2F);
        scheduleWave(player, targets, stats, 3, 0.8F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(7, () -> {
            playSound(player, BlackSouls.DOWN2_EVENT.get(), 1.1F);
            playSound(player, BlackSouls.SLASH4_EVENT.get(), 0.7F);
        }));
        scheduleWave(player, targets, stats, 10, 1.1F);
    }

    private void scheduleWave(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats, int delay, float pitch) {
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
            playSound(player, BlackSouls.SWORD4_EVENT.get(), pitch);
            playSound(player, BlackSouls.SWORD5_EVENT.get(), pitch);
            for (LivingEntity target : targets) {
                applyFormulaHit(player, target, stats, 5.0D, 2.0D, 0.2D, true, false, 0.0D);
            }
        }));
    }
}
