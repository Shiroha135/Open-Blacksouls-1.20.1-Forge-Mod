package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

@SuppressWarnings("removal")
public class SkillFistArt extends AbstractOriginalWeaponSkill {
    public enum Art { HAKI, HUNDRED_FISTS }
    private final Art art;

    public SkillFistArt(Art art) {
        super(Family.FIST, art == Art.HUNDRED_FISTS);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.HAKI ? "bs2_skill_haki" : "bs2_skill_hundred_fists"; }
    @Override public float getManaCost() { return art == Art.HAKI ? 0.0F : 15.0F; }
    @Override public int getBaseCooldownTicks() { return art == Art.HAKI ? 0 : 1000; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.YELLOW; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID,
            "textures/gui/skills/" + (art == Art.HAKI ? "haki.png" : "hundred_fists.png")); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.HAKI) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.haki.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            playAnimation(player, 95);
            playSound(player, BlackSouls.PUSH_EVENT.get(), 1.0F);
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(6, () -> {
                playSound(player, BlackSouls.RAISE1_EVENT.get(), 1.0F);
                player.addEffect(new MobEffectInstance(BlackSouls.BUFF_HAKI.get(), 1000, 0));
            }));
            return;
        }

        List<LivingEntity> targets = findTargets(player, 8.0D, 1);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        LivingEntity target = targets.get(0);
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.hundred_fists.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 241);
        playSound(target, BlackSouls.WIND10_EVENT.get(), 1.0F);
        for (int hit = 0; hit < 9; hit++) {
            int delay = 4 + hit * 2;
            boolean hasSound = hit < 8;
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                if (hasSound) playSound(target, BlackSouls.BLOW7_EVENT.get(), 0.5F);
                applyFormulaHit(player, target, stats, 2.0D, 2.0D, 0.2D, true, false, 0.0D);
            }));
        }
    }
}
