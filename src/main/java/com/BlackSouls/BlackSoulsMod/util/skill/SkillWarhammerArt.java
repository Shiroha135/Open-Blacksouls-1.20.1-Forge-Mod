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
import net.minecraft.world.entity.player.Player;

import java.util.List;

@SuppressWarnings("removal")
public class SkillWarhammerArt extends AbstractOriginalWeaponSkill {
    public enum Art { AIM, OVERHEAD_BARRAGE }
    private final Art art;

    public SkillWarhammerArt(Art art) {
        super(Family.WARHAMMER, art == Art.OVERHEAD_BARRAGE);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.AIM ? "bs2_skill_aim" : "bs2_skill_overhead_barrage"; }
    @Override public float getManaCost() { return art == Art.AIM ? 8.0F : 30.0F; }
    @Override public int getBaseCooldownTicks() { return art == Art.AIM ? 600 : 1000; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GOLD; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID,
            "textures/gui/skills/" + (art == Art.AIM ? "aim.png" : "overhead_barrage.png")); }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        return super.isWeaponEquipped(player)
                || (art == Art.AIM && player.getMainHandItem().getItem() == BlackSouls.HANS_MACHINE_GUN.get());
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.AIM) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.aim.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            playAnimation(player, 95);
            playSound(player, BlackSouls.PUSH_EVENT.get(), 1.0F);
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(6, () -> {
                playSound(player, BlackSouls.RAISE1_EVENT.get(), 1.0F);
                player.addEffect(new MobEffectInstance(BlackSouls.BUFF_AIM.get(), 600, 0));
            }));
            return;
        }

        List<LivingEntity> targets = findTargets(player, 8.0D, 1);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        LivingEntity target = targets.get(0);
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.overhead_barrage.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(target, 236);
        playSound(target, BlackSouls.EARTH5_EVENT.get(), 1.0F);
        for (int delay : new int[]{2, 4, 6}) {
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                playSound(target, BlackSouls.EARTH1_EVENT.get(), 1.0F);
                if (applyFormulaHit(player, target, stats, 3.0D, 2.0D, 0.2D, true, false, 0.0D)
                        && Math.random() < 0.20D) {
                    target.addEffect(new MobEffectInstance(BlackSouls.BUFF_STUN.get(), 40, 0));
                }
            }));
        }
    }
}
