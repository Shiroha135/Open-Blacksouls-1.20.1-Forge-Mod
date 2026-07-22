package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.util.BSAttributeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class SkillDarkSwordArt extends AbstractOriginalWeaponSkill {
    public enum Art { DARKNESS, SIN_CRUSH, SIN_BURST }
    private final Art art;

    public SkillDarkSwordArt(Art art) {
        super(Family.DARK_SWORD, art == Art.SIN_BURST);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case DARKNESS -> "bs2_skill_darkness";
        case SIN_CRUSH -> "bs2_skill_sin_crush";
        case SIN_BURST -> "bs2_skill_sin_burst";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case DARKNESS -> 20.0F;
        case SIN_CRUSH -> 10.0F;
        case SIN_BURST -> 20.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case DARKNESS -> 1200;
        case SIN_CRUSH -> 600;
        case SIN_BURST -> 1000;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_PURPLE; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case DARKNESS -> "darkness.png";
        case SIN_CRUSH -> "sin_crush.png";
        case SIN_BURST -> "sin_burst.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = art == Art.SIN_CRUSH
                ? findTargets(player, 8.0D, 1)
                : findTargets(player, 12.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = switch (art) {
            case DARKNESS -> "darkness";
            case SIN_CRUSH -> "sin_crush";
            case SIN_BURST -> "sin_burst";
        };
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        switch (art) {
            case DARKNESS -> executeDarkness(player, targets, stats);
            case SIN_CRUSH -> executeSinCrush(player, targets.get(0), stats);
            case SIN_BURST -> executeSinBurst(player, targets, stats);
        }
    }

    private void executeDarkness(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 78));
        playSound(player, BlackSouls.TELEPORT_EVENT.get(), 1.0F);
        for (LivingEntity target : targets) {
            double rawDamage = 500.0D + stats.magicAttack * 4.0D - StatEventHandler.getRpgMagicDefense(target) * 2.0D;
            rawDamage *= BSAttributeManager.getBestMultiplier(target, List.of(BSAttributeManager.DARK));
            rawDamage *= 0.8D + Math.random() * 0.4D;
            if (applyRawHit(player, target, rawDamage, true, false, 0.0D)) {
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_FEAR.get(), 1000, 0));
            }
        }
    }

    private void executeSinCrush(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 167);
        playSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0F);
        playSound(target, BlackSouls.SWORD3_EVENT.get(), 1.1F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(2, () -> {
            playSound(target, BlackSouls.DARKNESS4_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.ABSORB1_EVENT.get(), 0.7F);
            if (applyDarkHit(player, target, stats)) {
                if (Math.random() < 0.50D) target.addEffect(new MobEffectInstance(BlackSouls.BUFF_SEVERE_POISON.get(), 2000, 0));
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSELESS.get(), 400, 0));
            }
        }));
    }

    private void executeSinBurst(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 168));
        playSound(player, BlackSouls.SWORD5_EVENT.get(), 1.0F);
        playSound(player, BlackSouls.SWORD4_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(1, () -> {
            playSound(player, BlackSouls.ABSORB1_EVENT.get(), 0.5F);
            playSound(player, BlackSouls.DARKNESS7_EVENT.get(), 1.0F);
        }));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(2, () -> {
            for (LivingEntity target : targets) {
                if (applyDarkHit(player, target, stats) && Math.random() < 0.80D) {
                    target.addEffect(new MobEffectInstance(BlackSouls.BUFF_FRAGILE.get(), 800, 0));
                }
            }
        }));
    }

    private boolean applyDarkHit(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        double rawDamage = stats.attack * 6.0D - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
        rawDamage *= BSAttributeManager.getBestMultiplier(target, List.of(BSAttributeManager.DARK));
        rawDamage *= 0.8D + Math.random() * 0.4D;
        return applyRawHit(player, target, rawDamage, true, false, 0.0D);
    }
}
