package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class SkillRagnarokArt extends AbstractOriginalWeaponSkill {
    public enum Art { POMMEL_STUN, CLEAVE_IN_TWO, WRATH_OF_TWILIGHT }
    private final Art art;

    public SkillRagnarokArt(Art art) {
        super(Family.RAGNAROK_ROUTE, art == Art.WRATH_OF_TWILIGHT);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case POMMEL_STUN -> "bs2_skill_pommel_stun";
        case CLEAVE_IN_TWO -> "bs2_skill_cleave_in_two";
        case WRATH_OF_TWILIGHT -> "bs2_skill_wrath_of_twilight";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case POMMEL_STUN -> 10.0F;
        case CLEAVE_IN_TWO -> 20.0F;
        case WRATH_OF_TWILIGHT -> 90.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return 1000; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case POMMEL_STUN -> "pommel_stun.png";
        case CLEAVE_IN_TWO -> "cleave_in_two.png";
        case WRATH_OF_TWILIGHT -> "wrath_of_twilight.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = findTarget(player, 10.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        String key = switch (art) {
            case POMMEL_STUN -> "pommel_stun";
            case CLEAVE_IN_TWO -> "cleave_in_two";
            case WRATH_OF_TWILIGHT -> "wrath_of_twilight";
        };
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + key + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        switch (art) {
            case POMMEL_STUN -> executePommelStun(player, target, stats);
            case CLEAVE_IN_TWO -> executeCleave(player, target, stats);
            case WRATH_OF_TWILIGHT -> executeWrath(player, target, stats);
        }
    }

    private void executePommelStun(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 521);
        playSound(target, BlackSouls.EVASION1_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(2, () -> {
            playSound(target, BlackSouls.BLOW3_EVENT.get(), 0.5F);
            playSound(target, BlackSouls.BLOW5_EVENT.get(), 0.5F);
            if (applyFormulaHit(player, target, stats, 3.0D, 2.0D, 0.2D, true, true, 0.0D)) {
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_STUN.get(), 40, 0));
            }
        }));
    }

    private void executeCleave(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 522);
        playSound(target, BlackSouls.WIND7_EVENT.get(), 0.5F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(5, () -> {
            playSound(target, BlackSouls.BLOOD_SPLATTER_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.EARTH5_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SLASH9_EVENT.get(), 0.9F);
            applyRawHit(player, target, stats.attack * 5.0D
                    + target.getMaxHealth() * 0.05D * StatEventHandler.getPercentageDamageMultiplier(target), true, false, 0.0D);
        }));
    }

    private void executeWrath(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 523);
        playSound(target, BlackSouls.WIND7_EVENT.get(), 0.7F);
        playSound(target, BlackSouls.SAINT8_EVENT.get(), 1.1F);
        playSound(target, BlackSouls.WIND6_EVENT.get(), 0.7F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(13, () -> {
            playSound(target, BlackSouls.SKILL3_EVENT.get(), 1.0F);
            playSound(target, BlackSouls.SAINT9_EVENT.get(), 0.5F);
            if (applyFormulaHit(player, target, stats, 7.0D, 2.0D, 0.2D, true, true, 0.0D)) {
                List.copyOf(target.getActiveEffects()).stream()
                        .filter(effect -> effect.getEffect().isBeneficial())
                        .forEach(effect -> target.removeEffect(effect.getEffect()));
            }
        }));
    }
}
