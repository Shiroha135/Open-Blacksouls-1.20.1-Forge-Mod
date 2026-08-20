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
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

@SuppressWarnings("removal")
public class SkillCorruptScytheArt extends AbstractOriginalWeaponSkill {
    public enum Art { TWILIGHT_OF_GRUDGE, CORPSE_DRAGON_AWE, SOUL_COLLAPSE }
    private final Art art;

    public SkillCorruptScytheArt(Art art) {
        super(Family.CORRUPT_SCYTHE, art == Art.SOUL_COLLAPSE ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case TWILIGHT_OF_GRUDGE -> "bs2_skill_twilight_of_grudge";
        case CORPSE_DRAGON_AWE -> "bs2_skill_corpse_dragon_awe";
        case SOUL_COLLAPSE -> "bs2_skill_soul_collapse";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case TWILIGHT_OF_GRUDGE -> 50.0F;
        case CORPSE_DRAGON_AWE -> 60.0F;
        case SOUL_COLLAPSE -> 100.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case TWILIGHT_OF_GRUDGE -> 1000;
        case CORPSE_DRAGON_AWE -> 1600;
        case SOUL_COLLAPSE -> 4000;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.DARK_PURPLE; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case TWILIGHT_OF_GRUDGE -> "twilight_of_grudge.png";
        case CORPSE_DRAGON_AWE -> "corpse_dragon_awe.png";
        case SOUL_COLLAPSE -> "soul_collapse.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 12.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = switch (art) {
            case TWILIGHT_OF_GRUDGE -> "twilight_of_grudge";
            case CORPSE_DRAGON_AWE -> "corpse_dragon_awe";
            case SOUL_COLLAPSE -> "soul_collapse";
        };
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        switch (art) {
            case TWILIGHT_OF_GRUDGE -> executeTwilight(player, target);
            case CORPSE_DRAGON_AWE -> executeAwe(player, target);
            case SOUL_COLLAPSE -> executeCollapse(player, target);
        }
    }

    private void executeTwilight(ServerPlayer player, LivingEntity target) {
        playAnimation(target, 319);
        playSound(target, BlackSouls.DOWN2_EVENT.get(), 0.8F);
        scheduleSound(player, target, 14, BlackSouls.FOG1_EVENT.get(), 1.0F);
        scheduleSound(player, target, 14, BlackSouls.DARKNESS6_EVENT.get(), 0.7F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(14, () -> {
            if (applyPercentHit(player, target, 0.03D)) {
                StatEventHandler.applyAttackDown(target, 1000);
                StatEventHandler.applyMagicAttackDown(target, 1000);
            }
        }));
    }

    private void executeAwe(ServerPlayer player, LivingEntity target) {
        playAnimation(target, 320);
        playSound(target, BlackSouls.MONSTER4_EVENT.get(), 0.5F);
        scheduleSound(player, target, 15, BlackSouls.DARKNESS4_EVENT.get(), 0.7F);
        scheduleSound(player, target, 15, BlackSouls.COLLAPSE2_EVENT.get(), 0.8F);
        scheduleSound(player, target, 15, BlackSouls.DARKNESS1_EVENT.get(), 1.0F);
        for (int hit = 0; hit < 5; hit++) {
            int delay = 7 + hit * 3;
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> applyPercentHit(player, target, 0.01D)));
        }
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(19, () -> StatEventHandler.applySpeedDown(target, 1000)));
    }

    private void executeCollapse(ServerPlayer player, LivingEntity target) {
        playAnimation(target, 321);
        playSound(target, BlackSouls.POLLEN_EVENT.get(), 0.7F);
        playSound(target, BlackSouls.BLIND_EVENT.get(), 0.8F);
        scheduleSound(player, target, 10, BlackSouls.DARKNESS8_EVENT.get(), 0.9F);
        scheduleSound(player, target, 23, BlackSouls.DARKNESS7_EVENT.get(), 1.0F);
        scheduleSound(player, target, 28, BlackSouls.CRASH_EVENT.get(), 0.5F);
        scheduleSound(player, target, 28, BlackSouls.DARKNESS4_EVENT.get(), 0.8F);
        scheduleSound(player, target, 28, BlackSouls.DARKNESS5_EVENT.get(), 1.2F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(28, () -> applyPercentHit(player, target, 0.10D)));
    }

    private boolean applyPercentHit(ServerPlayer player, LivingEntity target, double maxHealthRate) {
        double rawDamage = 10.0D * (0.8D + Math.random() * 0.4D)
                * BSAttributeManager.getBestMultiplier(target, List.of(BSAttributeManager.DARK))
                + target.getMaxHealth() * maxHealthRate * StatEventHandler.getPercentageDamageMultiplier(target);
        return applyRawHit(player, target, rawDamage, false, true, 0.0D);
    }

    private void scheduleSound(ServerPlayer player, LivingEntity target, int delay, net.minecraft.sounds.SoundEvent sound, float pitch) {
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
            if (!target.isRemoved()) playSound(target, sound, pitch);
        }));
    }
}
