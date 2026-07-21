package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class SkillSmolderingFrenzy extends AbstractBandersnatchSkill {
    private static final String TAG_CHARGE = "bs2_bandersnatch_charge";

    @Override
    public String getSkillId() { return "bs2_skill_smoldering_frenzy"; }

    @Override
    public float getManaCost() { return 10.0F; }

    @Override
    public int getBaseCooldownTicks() { return 0; }

    @Override
    public String getTranslationKey() { return "skill.blacksouls.bs2_skill_smoldering_frenzy.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.RED; }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/smoldering_frenzy.png");
    }

    @Override
    public boolean canCast(ServerPlayer player, BSPlayerStats stats) {
        if (getCharge(player) < 9) {
            return super.canCast(player, stats);
        }
        if (!isWeaponEquipped(player)) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.wrong_weapon").withStyle(ChatFormatting.RED));
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

    @Override
    public void consumeAndSetCooldown(ServerPlayer player, BSPlayerStats stats) {
        if (getCharge(player) >= 9) {
            SkillUtils.consumeActionPoints(player, getActionCost());
            StatEventHandler.syncToClient(player);
            return;
        }
        super.consumeAndSetCooldown(player, stats);
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        int charge = getCharge(player);
        if (charge >= 9) {
            release(player, stats);
            return;
        }

        int nextCharge = charge + 1;
        SkillUtils.getPersistedData(player).putInt(TAG_CHARGE, nextCharge);
        player.sendSystemMessage(Component.translatable(
                "message.blacksouls.skill.smoldering_frenzy.charge",
                player.getName().getString(),
                nextCharge,
                9
        ).withStyle(ChatFormatting.WHITE));
        playAnimation(player, 316);
        playSound(player, BlackSouls.FIRE1_EVENT.get(), 0.5F);
        playSound(player, BlackSouls.HEARTBEAT_EVENT.get(), 1.0F);
    }

    private void release(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = findTargets(player, 16.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        SkillUtils.getPersistedData(player).remove(TAG_CHARGE);
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.smoldering_frenzy.release", player.getName().getString()).withStyle(ChatFormatting.DARK_RED));
        player.swing(InteractionHand.MAIN_HAND, true);
        playSound(player, BlackSouls.THUNDER7_EVENT.get(), 0.5F);
        playSound(player, BlackSouls.BATTLE3_EVENT.get(), 1.0F);
        for (LivingEntity target : targets) {
            playAnimation(target, 317);
            applyFormulaHit(player, target, stats, 100.0D, 2.0D, 0.2D, true, false, 0.0D);
        }
    }

    private int getCharge(ServerPlayer player) {
        return Math.max(0, Math.min(9, SkillUtils.getPersistedData(player).getInt(TAG_CHARGE)));
    }
}
