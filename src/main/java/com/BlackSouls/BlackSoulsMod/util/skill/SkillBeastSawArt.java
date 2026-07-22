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

public class SkillBeastSawArt extends AbstractOriginalWeaponSkill {
    public enum Art { FLESH_CARVE, BLOOD_TRAIL, BLOOD_EDGE }
    private final Art art;

    public SkillBeastSawArt(Art art) {
        super(Family.BEAST_SAW, art == Art.BLOOD_EDGE);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case FLESH_CARVE -> "bs2_skill_flesh_carve";
        case BLOOD_TRAIL -> "bs2_skill_blood_trail";
        case BLOOD_EDGE -> "bs2_skill_blood_edge";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case FLESH_CARVE -> 10.0F;
        case BLOOD_TRAIL -> 6.0F;
        case BLOOD_EDGE -> 30.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case FLESH_CARVE -> 600;
        case BLOOD_TRAIL -> 1000;
        case BLOOD_EDGE -> 1600;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_RED; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case FLESH_CARVE -> "flesh_carve.png";
        case BLOOD_TRAIL -> "blood_trail.png";
        case BLOOD_EDGE -> "blood_edge.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.BLOOD_TRAIL) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.blood_trail.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            playAnimation(player, 95);
            playSound(player, BlackSouls.PUSH_EVENT.get(), 1.0F);
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(6, () -> {
                playSound(player, BlackSouls.RAISE1_EVENT.get(), 1.0F);
                StatEventHandler.applySpeedUp(player, 1000);
                StatEventHandler.applySpeedUp(player, 1000);
            }));
            return;
        }

        List<LivingEntity> targets = art == Art.FLESH_CARVE
                ? findTargets(player, 8.0D, 1)
                : findTargets(player, 12.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = art == Art.FLESH_CARVE ? "flesh_carve" : "blood_edge";
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        if (art == Art.FLESH_CARVE) executeFleshCarve(player, targets.get(0), stats);
        else executeBloodEdge(player, targets, stats);
    }

    private void executeFleshCarve(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 233);
        playSound(target, BlackSouls.TWINE_EVENT.get(), 1.2F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(6, () -> {
            playSound(target, BlackSouls.SWORD5_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SLASH2_EVENT.get(), 1.0F);
            double rawDamage = stats.attack * 4.0D - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
            rawDamage *= BSAttributeManager.getBestMultiplier(target, List.of(BSAttributeManager.BEAST_KILLER));
            rawDamage *= 0.8D + Math.random() * 0.4D;
            if (applyRawHit(player, target, rawDamage, true, true, 0.0D)) {
                if (Math.random() < 0.70D) target.addEffect(new MobEffectInstance(BlackSouls.BUFF_BLEEDING.get(), 600, 0));
                target.removeEffect(BlackSouls.BUFF_DAGGER_EVASION.get());
            }
        }));
    }

    private void executeBloodEdge(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 234));
        playSlashSequence(player);
        for (int wave = 0; wave < 2; wave++) {
            int delay = 8 + wave * 6;
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                for (LivingEntity target : targets) {
                    boolean bleeding = target.hasEffect(BlackSouls.BUFF_BLEEDING.get());
                    double rawDamage = stats.attack * 8.0D - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
                    if (bleeding) rawDamage *= 2.0D;
                    rawDamage *= 0.8D + Math.random() * 0.4D;
                    if (applyRawHit(player, target, rawDamage, true, true, 0.0D) && Math.random() < 0.40D) {
                        target.addEffect(new MobEffectInstance(BlackSouls.BUFF_BLEEDING.get(), 600, 0));
                    }
                }
            }));
        }
    }

    private void playSlashSequence(ServerPlayer player) {
        net.minecraft.sounds.SoundEvent[] sounds = {
                BlackSouls.SLASH1_EVENT.get(), BlackSouls.SLASH2_EVENT.get(), BlackSouls.SLASH3_EVENT.get(),
                BlackSouls.SLASH4_EVENT.get(), BlackSouls.SLASH5_EVENT.get(), BlackSouls.SLASH6_EVENT.get(),
                BlackSouls.SLASH7_EVENT.get(), BlackSouls.SLASH8_EVENT.get(), BlackSouls.SLASH9_EVENT.get()
        };
        for (int i = 0; i < sounds.length; i++) {
            int delay = i;
            net.minecraft.sounds.SoundEvent sound = sounds[i];
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> playSound(player, sound, 1.0F)));
        }
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(14, () -> {
            playSound(player, BlackSouls.SLASH2_EVENT.get(), 0.8F);
            playSound(player, BlackSouls.SLASH9_EVENT.get(), 0.5F);
        }));
    }
}
