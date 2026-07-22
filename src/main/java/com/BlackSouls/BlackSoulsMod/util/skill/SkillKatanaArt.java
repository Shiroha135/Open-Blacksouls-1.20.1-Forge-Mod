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

public class SkillKatanaArt extends AbstractOriginalWeaponSkill {
    public enum Art { IAI, FORWARD_SLASH, TEMPEST_REND }
    private final Art art;

    public SkillKatanaArt(Art art) {
        super(Family.KATANA, art == Art.TEMPEST_REND);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case IAI -> "bs2_skill_iai";
        case FORWARD_SLASH -> "bs2_skill_forward_slash";
        case TEMPEST_REND -> "bs2_skill_tempest_rend";
    }; }
    @Override public float getManaCost() { return 0.0F; }
    @Override public int getBaseCooldownTicks() { return 0; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.RED; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/katana_art.png"); }

    @Override
    public boolean canCast(ServerPlayer player, BSPlayerStats stats) {
        if (!super.canCast(player, stats)) return false;
        float healthCost = player.getMaxHealth() * getHealthCostRate();
        if (player.getHealth() <= healthCost) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.not_enough_hp").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> targets = art == Art.TEMPEST_REND
                ? findTargets(player, 12.0D, Integer.MAX_VALUE)
                : findTargets(player, 8.0D, 1);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.setHealth(player.getHealth() - player.getMaxHealth() * getHealthCostRate());
        String key = switch (art) {
            case IAI -> "iai";
            case FORWARD_SLASH -> "forward_slash";
            case TEMPEST_REND -> "tempest_rend";
        };
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        switch (art) {
            case IAI -> executeIai(player, targets.get(0), stats);
            case FORWARD_SLASH -> executeForwardSlash(player, targets.get(0), stats);
            case TEMPEST_REND -> executeTempestRend(player, targets, stats);
        }
    }

    private float getHealthCostRate() {
        return art == Art.IAI ? 0.05F : 0.10F;
    }

    private void executeIai(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 337);
        playSound(target, BlackSouls.DAO2_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(8, () -> {
            playSound(target, BlackSouls.DAO_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SWORD5_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SWORD3_EVENT.get(), 1.0F);
        }));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(10, () -> {
            playSound(target, BlackSouls.DAO3_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SKILL2_EVENT.get(), 1.0F);
            applyFormulaHit(player, target, stats, 6.0D, 1.0D, 0.2D, true, true, 0.0D);
        }));
    }

    private void executeForwardSlash(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 338);
        playSound(target, BlackSouls.DAO2_EVENT.get(), 1.15F);
        scheduleSound(player, target, 3, BlackSouls.SLASH2_EVENT.get(), 1.0F);
        scheduleSoundPair(player, target, 6, BlackSouls.SLASH3_EVENT.get(), 1.5F, BlackSouls.SWORD4_EVENT.get(), 1.0F);
        scheduleSoundPair(player, target, 8, BlackSouls.SLASH3_EVENT.get(), 1.5F, BlackSouls.SWORD5_EVENT.get(), 1.0F);
        scheduleSoundPair(player, target, 10, BlackSouls.SLASH3_EVENT.get(), 1.5F, BlackSouls.SWORD4_EVENT.get(), 1.0F);
        scheduleSoundPair(player, target, 12, BlackSouls.SLASH3_EVENT.get(), 1.5F, BlackSouls.SWORD5_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(14, () -> {
            playSound(target, BlackSouls.DAO3_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SLASH3_EVENT.get(), 1.5F);
            playSound(target, BlackSouls.SWORD5_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SWORD4_EVENT.get(), 1.0F);
        }));
        for (int delay : new int[]{3, 8, 14}) {
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                if (applyFormulaHit(player, target, stats, 3.0D, 1.0D, 0.2D, true, false, 0.0D)
                        && Math.random() < 0.50D) {
                    target.addEffect(new MobEffectInstance(BlackSouls.BUFF_BLEEDING.get(), 600, 0));
                }
            }));
        }
    }

    private void executeTempestRend(ServerPlayer player, List<LivingEntity> targets, BSPlayerStats stats) {
        targets.forEach(target -> playAnimation(target, 339));
        playSound(player, BlackSouls.DAO2_EVENT.get(), 1.0F);
        for (int delay : new int[]{6, 10}) {
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                playSound(player, BlackSouls.DAO_EVENT.get(), 1.0F);
                for (LivingEntity target : targets) {
                    if (applyFormulaHit(player, target, stats, 5.0D, 0.5D, 0.2D, true, false, 0.0D)
                            && Math.random() < 0.50D) {
                        target.addEffect(new MobEffectInstance(BlackSouls.BUFF_STUN.get(), 40, 0));
                    }
                }
            }));
        }
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(12, () -> playSound(player, BlackSouls.DAO3_EVENT.get(), 1.0F)));
    }

    private void scheduleSound(ServerPlayer player, LivingEntity target, int delay, net.minecraft.sounds.SoundEvent sound, float pitch) {
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> playSound(target, sound, pitch)));
    }

    private void scheduleSoundPair(ServerPlayer player, LivingEntity target, int delay,
                                   net.minecraft.sounds.SoundEvent first, float firstPitch,
                                   net.minecraft.sounds.SoundEvent second, float secondPitch) {
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
            playSound(target, first, firstPitch);
            playSound(target, second, secondPitch);
        }));
    }
}
