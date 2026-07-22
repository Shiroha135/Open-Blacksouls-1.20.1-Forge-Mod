package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class SkillCleaverAxeArt extends AbstractOriginalWeaponSkill {
    public enum Art { SLASH_DOWN, MASSACRE_AXE, GIANT_GUILLOTINE }
    private final Art art;

    public SkillCleaverAxeArt(Art art) {
        super(Family.CLEAVER_AXE, art == Art.GIANT_GUILLOTINE);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case SLASH_DOWN -> "bs2_skill_slash_down";
        case MASSACRE_AXE -> "bs2_skill_massacre_axe";
        case GIANT_GUILLOTINE -> "bs2_skill_giant_guillotine";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case SLASH_DOWN -> 10.0F;
        case MASSACRE_AXE -> 20.0F;
        case GIANT_GUILLOTINE -> 30.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case SLASH_DOWN -> 600;
        case MASSACRE_AXE -> 1000;
        case GIANT_GUILLOTINE -> 1600;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_RED; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case SLASH_DOWN -> "slash_down.png";
        case MASSACRE_AXE -> "massacre_axe.png";
        case GIANT_GUILLOTINE -> "giant_guillotine.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = switch (art) {
            case SLASH_DOWN -> findTargets(player, 8.0D, 1);
            case MASSACRE_AXE -> findTargets(player, 10.0D, 3);
            case GIANT_GUILLOTINE -> findTargets(player, 12.0D, Integer.MAX_VALUE);
        };
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = switch (art) {
            case SLASH_DOWN -> "slash_down";
            case MASSACRE_AXE -> "massacre_axe";
            case GIANT_GUILLOTINE -> "giant_guillotine";
        };
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        switch (art) {
            case SLASH_DOWN -> executeSlashDown(player, targets.get(0), stats);
            case MASSACRE_AXE -> executeMassacre(player, targets, stats);
            case GIANT_GUILLOTINE -> executeGuillotine(player, targets, stats);
        }
    }

    private void executeSlashDown(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 130);
        playSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(2, () -> {
            if (applyFormulaHit(player, target, stats, 5.0D, 2.0D, 0.2D, false, true, 0.0D)) {
                StatEventHandler.applySpeedDown(target, 1000);
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_FEAR.get(), 100, 0));
            }
        }));
    }

    private void executeMassacre(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 134));
        playSound(player, BlackSouls.SLASH2_EVENT.get(), 1.5F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(3, () -> playSound(player, BlackSouls.SLASH2_EVENT.get(), 1.5F)));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(6, () -> playSound(player, BlackSouls.SLASH2_EVENT.get(), 1.5F)));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(9, () -> {
            playSound(player, BlackSouls.SLASH9_EVENT.get(), 1.0F);
            for (LivingEntity target : targets) {
                if (applyFormulaHit(player, target, stats, 2.0D, 2.0D, 0.3D, true, false, 0.0D)) {
                    StatEventHandler.applyDefenseDown(target, 2000);
                    StatEventHandler.applyDefenseDown(target, 2000);
                }
            }
        }));
    }

    private void executeGuillotine(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 133));
        playSound(player, BlackSouls.WIND10_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(2, () -> {
            playSound(player, BlackSouls.SLASH2_EVENT.get(), 1.0F);
            playSound(player, BlackSouls.SWORD3_EVENT.get(), 1.0F);
            playSound(player, BlackSouls.SWORD4_EVENT.get(), 1.0F);
            playSound(player, BlackSouls.SWORD5_EVENT.get(), 1.0F);
            for (LivingEntity target : targets) {
                if (applyFormulaHit(player, target, stats, 5.0D, 2.0D, 0.2D, true, false, 0.0D)) {
                    target.addEffect(new MobEffectInstance(BlackSouls.BUFF_FEAR.get(), 100, 0));
                }
            }
        }));
    }
}
