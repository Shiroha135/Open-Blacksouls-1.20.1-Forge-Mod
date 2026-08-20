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

@SuppressWarnings("removal")
public class SkillHalberdArt extends AbstractOriginalWeaponSkill {
    public enum Art { COUNTER, MENTAL_BREAK, SILVER_MOON_THUNDER_AXE }
    private final Art art;

    public SkillHalberdArt(Art art) {
        super(Family.HALBERD, art == Art.SILVER_MOON_THUNDER_AXE);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case COUNTER -> "bs2_skill_counter";
        case MENTAL_BREAK -> "bs2_skill_mental_break";
        case SILVER_MOON_THUNDER_AXE -> "bs2_skill_silver_moon_thunder_axe";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case COUNTER -> 5.0F;
        case MENTAL_BREAK -> 15.0F;
        case SILVER_MOON_THUNDER_AXE -> 30.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case COUNTER -> 0;
        case MENTAL_BREAK -> 1000;
        case SILVER_MOON_THUNDER_AXE -> 1600;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case COUNTER -> "counter.png";
        case MENTAL_BREAK -> "mental_break.png";
        case SILVER_MOON_THUNDER_AXE -> "silver_moon_thunder_axe.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.COUNTER) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.counter.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_COUNTER_STANCE.get(), 400, 0));
            return;
        }

        List<LivingEntity> targets = findTargets(player, art == Art.MENTAL_BREAK ? 8.0D : 12.0D,
                art == Art.MENTAL_BREAK ? 1 : 3);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + (art == Art.MENTAL_BREAK ? "mental_break" : "silver_moon_thunder_axe") + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        if (art == Art.MENTAL_BREAK) executeMentalBreak(player, targets.get(0), stats);
        else executeSilverMoon(player, targets, stats);
    }

    private void executeMentalBreak(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 139);
        playSound(target, BlackSouls.MAGIC1_EVENT.get(), 1.2F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(4, () -> playSound(target, BlackSouls.MAGIC4_EVENT.get(), 1.0F)));
        for (int wave = 0; wave < 5; wave++) {
            int delay = 7 + wave * 2;
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                playSound(target, BlackSouls.CRASH_EVENT.get(), 1.5F);
                playSound(target, BlackSouls.ATTACK3_EVENT.get(), 1.0F);
                double rawDamage = stats.attack + stats.magicAttack
                        - StatEventHandler.getRpgPhysicalDefense(target) * 0.5D
                        - StatEventHandler.getRpgMagicDefense(target) * 0.5D;
                rawDamage *= 0.8D + Math.random() * 0.4D;
                if (applyRawHit(player, target, rawDamage, true, false, 0.0D)) {
                    StatEventHandler.applyMagicDefenseDown(target, 2000);
                }
            }));
        }
    }

    private void executeSilverMoon(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 231));
        playSound(player, BlackSouls.SWORD3_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(2, () -> {
            playSound(player, BlackSouls.SWORD1_EVENT.get(), 1.0F);
            playSound(player, BlackSouls.SLASH1_EVENT.get(), 1.0F);
        }));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(8, () -> {
            playSound(player, BlackSouls.SLASH2_EVENT.get(), 1.0F);
            playSound(player, BlackSouls.DOWN2_EVENT.get(), 1.0F);
            for (LivingEntity target : targets) {
                double rawDamage = stats.attack * 5.0D + stats.magicAttack * 5.0D
                        - StatEventHandler.getRpgPhysicalDefense(target) * 0.5D
                        - StatEventHandler.getRpgMagicDefense(target) * 0.5D;
                rawDamage *= 0.8D + Math.random() * 0.4D;
                applyRawHit(player, target, rawDamage, true, false, 0.0D);
            }
        }));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(17, () -> {
            playSound(player, BlackSouls.SWORD4_EVENT.get(), 1.0F);
            playSound(player, BlackSouls.SLASH9_EVENT.get(), 1.0F);
        }));
    }
}
