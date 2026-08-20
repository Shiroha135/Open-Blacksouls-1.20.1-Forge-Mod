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
public class SkillBrokenSwordArt extends AbstractOriginalWeaponSkill {
    public enum Art { SELF_HARM, DEAD_STRIKE }
    private final Art art;

    public SkillBrokenSwordArt(Art art) {
        super(Family.BROKEN_SWORD, art == Art.DEAD_STRIKE);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.SELF_HARM ? "bs2_skill_self_harm" : "bs2_skill_dead_strike"; }
    @Override public float getManaCost() { return art == Art.SELF_HARM ? 3.0F : 5.0F; }
    @Override public int getBaseCooldownTicks() { return art == Art.SELF_HARM ? 600 : 1600; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.RED; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID,
            "textures/gui/skills/" + (art == Art.SELF_HARM ? "self_harm.png" : "dead_strike.png")); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.SELF_HARM) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.self_harm.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            playAnimation(player, 180);
            playSound(player, BlackSouls.SWORD_STAB_EVENT.get(), 1.4F);
            double rawDamage = (stats.attack * 2.0D - stats.defense) * (0.8D + Math.random() * 0.4D);
            if (applyRawHit(player, player, rawDamage, false, true, 0.0D)) {
                player.addEffect(new MobEffectInstance(BlackSouls.BUFF_BLEEDING.get(), 600, 0));
                player.addEffect(new MobEffectInstance(BlackSouls.BUFF_SELF_HARM.get(), 400, 0));
            }
            return;
        }

        List<LivingEntity> targets = findTargets(player, 8.0D, 1);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        LivingEntity target = targets.get(0);
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.dead_strike.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 181);
        playSound(target, BlackSouls.SLASH2_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(2, () -> {
            playSound(target, BlackSouls.GUCHA004A_EVENT.get(), 0.75F);
            double baseDamage = stats.attack * 4.0D - StatEventHandler.getRpgPhysicalDefense(target) * 2.0D;
            double healthMultiplier = Math.min(player.getMaxHealth() / Math.max(1.0D, player.getHealth()), 10.0D);
            double rawDamage = baseDamage * healthMultiplier * (0.8D + Math.random() * 0.4D);
            applyRawHit(player, target, rawDamage, true, false, 0.0D);
        }));
    }
}
