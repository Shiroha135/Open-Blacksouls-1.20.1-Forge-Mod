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

public class SkillSweep extends AbstractGreatSwordSkill {

    @Override
    public String getSkillId() { return "bs2_skill_sweep"; }

    @Override
    public float getManaCost() { return 5.0F; }

    @Override
    public int getBaseCooldownTicks() { return 0; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_sweep.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/sweep.png");
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = findTargets(player, 6.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.sweep.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        for (LivingEntity target : targets) {
            playAnimation(target, 127);
        }
        playSound(player, BlackSouls.SLASH2_EVENT.get(), 1.0F);

        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(3, () -> {
            playSound(player, BlackSouls.SLASH9_EVENT.get(), 1.0F);
            playSound(player, BlackSouls.BLOW7_EVENT.get(), 0.5F);
            for (LivingEntity target : targets) {
                applyFormulaHit(player, target, stats, 4.0D, 2.0D, 0.25D, true, false, 0.0D);
            }
        }));
    }
}
