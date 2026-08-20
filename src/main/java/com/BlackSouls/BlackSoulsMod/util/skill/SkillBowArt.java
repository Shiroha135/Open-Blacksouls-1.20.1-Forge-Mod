package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

@SuppressWarnings("removal")
public class SkillBowArt extends AbstractOriginalWeaponSkill {
    public enum Art { FOOT_SHOT, TRIPLE_SHOT, ARROW_RAIN }
    private final Art art;

    public SkillBowArt(Art art) {
        super(Family.BOW, art == Art.ARROW_RAIN);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case FOOT_SHOT -> "bs2_skill_foot_shot";
        case TRIPLE_SHOT -> "bs2_skill_triple_shot";
        case ARROW_RAIN -> "bs2_skill_arrow_rain";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case FOOT_SHOT -> 10.0F;
        case TRIPLE_SHOT -> 25.0F;
        case ARROW_RAIN -> 50.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case FOOT_SHOT -> 600;
        case TRIPLE_SHOT -> 1000;
        case ARROW_RAIN -> 1600;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GREEN; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case FOOT_SHOT -> "foot_shot.png";
        case TRIPLE_SHOT -> "triple_shot.png";
        case ARROW_RAIN -> "arrow_rain.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = switch (art) {
            case FOOT_SHOT -> findTargets(player, 16.0D, 1);
            case TRIPLE_SHOT -> findTargets(player, 16.0D, 3);
            case ARROW_RAIN -> findTargets(player, 16.0D, Integer.MAX_VALUE);
        };
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + switch (art) {
            case FOOT_SHOT -> "foot_shot";
            case TRIPLE_SHOT -> "triple_shot";
            case ARROW_RAIN -> "arrow_rain";
        } + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        switch (art) {
            case FOOT_SHOT -> executeFootShot(player, targets.get(0), stats);
            case TRIPLE_SHOT -> executeTripleShot(player, targets, stats);
            case ARROW_RAIN -> executeArrowRain(player, targets, stats);
        }
    }

    private void executeFootShot(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 226);
        playBowDrawSounds(player);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(8, () -> {
            playArrowImpactSounds(target);
            if (applyFormulaHit(player, target, stats, 4.0D, 2.0D, 0.2D, true, true, 0.0D)) {
                StatEventHandler.applySpeedDown(target, 1000);
            }
        }));
    }

    private void executeTripleShot(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 227));
        playBowDrawSounds(player);
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity target = targets.get(i);
            int delay = 8 + i * 3;
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                playArrowImpactSounds(target);
                double rawDamage = stats.attack * 2.0D + stats.speed * 2.0D
                        - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
                rawDamage *= 0.8D + Math.random() * 0.4D;
                applyRawHit(player, target, rawDamage, true, false, 0.0D);
            }));
        }
    }

    private void executeArrowRain(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 225));
        playSound(player, BlackSouls.BOW1_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(2, () -> playSound(player, BlackSouls.BOW2_EVENT.get(), 1.0F)));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(4, () -> playSound(player, BlackSouls.BOW1_EVENT.get(), 1.0F)));
        for (int wave = 0; wave < 3; wave++) {
            int delay = 8 + wave * 4;
            int pitchStep = wave;
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                playSound(player, pitchStep == 1 ? BlackSouls.SLASH10_EVENT.get() : BlackSouls.SLASH11_EVENT.get(), 0.9F + pitchStep * 0.1F);
                for (LivingEntity target : targets) {
                    double rawDamage = stats.attack * 3.0D + stats.speed * 2.0D
                            - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
                    rawDamage *= 0.8D + Math.random() * 0.4D;
                    applyRawHit(player, target, rawDamage, true, false, 0.0D);
                }
            }));
        }
    }

    private void playBowDrawSounds(ServerPlayer player) {
        playSound(player, BlackSouls.BOW4_EVENT.get(), 1.5F);
    }

    private void playArrowImpactSounds(LivingEntity target) {
        playSound(target, BlackSouls.SLASH10_EVENT.get(), 1.0F);
        playSound(target, BlackSouls.BOW2_EVENT.get(), 0.8F);
        playSound(target, BlackSouls.BOW1_EVENT.get(), 1.0F);
    }
}
