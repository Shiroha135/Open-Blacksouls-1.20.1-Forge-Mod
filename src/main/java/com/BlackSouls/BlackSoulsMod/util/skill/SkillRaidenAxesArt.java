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
public class SkillRaidenAxesArt extends AbstractOriginalWeaponSkill {
    public enum Art { HEAVEN_SHATTERING_THUNDER, LION_WHIRLWIND }
    private final Art art;

    public SkillRaidenAxesArt(Art art) {
        super(Family.RAIDEN_AXES, art == Art.LION_WHIRLWIND ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.HEAVEN_SHATTERING_THUNDER ? "bs2_skill_heaven_shattering_thunder" : "bs2_skill_lion_whirlwind"; }
    @Override public float getManaCost() { return art == Art.HEAVEN_SHATTERING_THUNDER ? 80.0F : 120.0F; }
    @Override public int getBaseCooldownTicks() { return art == Art.HEAVEN_SHATTERING_THUNDER ? 600 : 1800; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.YELLOW; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + (art == Art.HEAVEN_SHATTERING_THUNDER ? "heaven_shattering_thunder.png" : "lion_whirlwind.png")); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.HEAVEN_SHATTERING_THUNDER) {
            LivingEntity target = findTarget(player, 14.0D);
            if (target == null) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
                return;
            }
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.heaven_shattering_thunder.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            playAnimation(target, 553);
            playSound(target, BlackSouls.MAGIC5_EVENT.get(), 1.0F);
            schedule(player, 12, () -> playSound(target, BlackSouls.BLOW5_EVENT.get(), 0.5F));
            schedule(player, 16, () -> {
                playSound(target, BlackSouls.THUNDER10_EVENT.get(), 1.5F);
                playSound(target, BlackSouls.THUNDER6_EVENT.get(), 0.5F);
                hit(player, target, true);
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEF_DOWN.get(), 1000, 0));
            });
        } else {
            List<LivingEntity> targets = findTargets(player, 16.0D, Integer.MAX_VALUE);
            if (targets.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
                return;
            }
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.lion_whirlwind.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            executeVolley(player, targets, 0);
        }
    }

    private void executeVolley(ServerPlayer player, List<LivingEntity> targets, int chain) {
        targets.forEach(target -> playAnimation(target, 552));
        playSound(player, BlackSouls.SLASH1_EVENT.get(), 0.8F);
        schedule(player, 3, () -> playSound(player, BlackSouls.SLASH5_EVENT.get(), 1.0F));
        schedule(player, 6, () -> playSound(player, BlackSouls.SLASH6_EVENT.get(), 0.85F));
        schedule(player, 9, () -> playSound(player, BlackSouls.SLASH7_EVENT.get(), 0.8F));
        schedule(player, 12, () -> playSound(player, BlackSouls.EARTH5_EVENT.get(), 0.75F));
        for (int strike = 0; strike < 3; strike++) {
            int delay = 2 + strike * 4;
            schedule(player, delay, () -> {
                for (LivingEntity target : targets) {
                    if (hit(player, target, false)) {
                        double heal = Math.max(1.0D, player.getHealth() * 0.1D - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D);
                        player.heal((float) heal);
                    }
                }
            });
        }
        if (chain < 7) {
            schedule(player, 16, () -> {
                if (player.getRandom().nextDouble() < 0.50D) executeVolley(player, targets, chain + 1);
            });
        }
    }

    private boolean hit(ServerPlayer player, LivingEntity target, boolean sureHit) {
        double rawDamage = player.getHealth() * 0.1D - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
        rawDamage *= 0.8D + Math.random() * 0.4D;
        return applyRawHit(player, target, rawDamage, false, sureHit, 0.0D);
    }

    private static void schedule(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }
}
