package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class SkillIronBallArt extends AbstractOriginalWeaponSkill {
    public enum Art { CHAKRA, RECKLESS_STRIKE, DOUBLE_COLLISION }
    private final Art art;

    public SkillIronBallArt(Art art) {
        super(Family.IRON_BALL, art == Art.DOUBLE_COLLISION ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case CHAKRA -> "bs2_skill_chakra";
        case RECKLESS_STRIKE -> "bs2_skill_reckless_strike";
        case DOUBLE_COLLISION -> "bs2_skill_double_collision";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case CHAKRA -> 1.0F;
        case RECKLESS_STRIKE -> 20.0F;
        case DOUBLE_COLLISION -> 30.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case CHAKRA -> 0;
        case RECKLESS_STRIKE -> 1000;
        case DOUBLE_COLLISION -> 1600;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case CHAKRA -> "chakra.png";
        case RECKLESS_STRIKE -> "reckless_strike.png";
        case DOUBLE_COLLISION -> "double_collision.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.CHAKRA) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.chakra.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            playAnimation(player, 38);
            playSound(player, BlackSouls.ICE8_EVENT.get(), 1.0F);
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(9, () -> {
                playSound(player, BlackSouls.SAINT7_EVENT.get(), 1.5F);
                for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                    if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) player.removeEffect(effect.getEffect());
                }
            }));
            return;
        }

        List<LivingEntity> targets = art == Art.RECKLESS_STRIKE
                ? findTargets(player, 10.0D, 3)
                : findTargets(player, 8.0D, 1);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = art == Art.RECKLESS_STRIKE ? "reckless_strike" : "double_collision";
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        if (art == Art.RECKLESS_STRIKE) executeReckless(player, targets, stats);
        else executeDoubleCollision(player, targets.get(0), stats);
    }

    private void executeReckless(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 5));
        int[] blow1 = {0, 2, 5, 9};
        int[] blow3 = {3, 12, 14};
        int[] blow5 = {7, 17};
        for (int delay : blow1) scheduleSound(player, delay, BlackSouls.BLOW1_EVENT.get());
        for (int delay : blow3) scheduleSound(player, delay, BlackSouls.BLOW3_EVENT.get());
        for (int delay : blow5) scheduleSound(player, delay, BlackSouls.BLOW5_EVENT.get());
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(17, () -> {
            for (LivingEntity target : targets) {
                if (applyFormulaHit(player, target, stats, 4.0D, 2.0D, 0.5D, true, false, 0.0D)) {
                    StatEventHandler.applyAttackDown(target, 1000);
                }
            }
        }));
    }

    private void executeDoubleCollision(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 129);
        playSound(target, BlackSouls.DOWN2_EVENT.get(), 1.5F);
        for (int delay : new int[]{0, 7}) {
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                if (delay == 7) playSound(target, BlackSouls.BLOW7_EVENT.get(), 0.5F);
                double rawDamage = stats.attack * 6.0D * (0.8D + Math.random() * 0.4D);
                applyRawHit(player, target, rawDamage, true, false, 0.0D);
            }));
        }
    }

    private void scheduleSound(ServerPlayer player, int delay, net.minecraft.sounds.SoundEvent sound) {
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> playSound(player, sound, 1.0F)));
    }
}
