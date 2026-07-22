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

public class SkillEuniceRapierArt extends AbstractOriginalWeaponSkill {
    public enum Art { SKY_CLEAVING_SLASH, MIND_EYE, PEERLESS_CHALLENGE }
    private final Art art;

    public SkillEuniceRapierArt(Art art) {
        super(Family.EUNICE_RAPIER, art == Art.PEERLESS_CHALLENGE ? 5 : 0);
        this.art = art;
    }

    @Override public String getSkillId() { return switch (art) {
        case SKY_CLEAVING_SLASH -> "bs2_skill_sky_cleaving_slash";
        case MIND_EYE -> "bs2_skill_mind_eye";
        case PEERLESS_CHALLENGE -> "bs2_skill_peerless_challenge";
    }; }
    @Override public float getManaCost() { return switch (art) {
        case SKY_CLEAVING_SLASH -> 20.0F;
        case MIND_EYE -> 50.0F;
        case PEERLESS_CHALLENGE -> 100.0F;
    }; }
    @Override public int getBaseCooldownTicks() { return switch (art) {
        case SKY_CLEAVING_SLASH -> 600;
        case MIND_EYE -> 4800;
        case PEERLESS_CHALLENGE -> 2000;
    }; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.AQUA; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/" + switch (art) {
        case SKY_CLEAVING_SLASH -> "sky_cleaving_slash.png";
        case MIND_EYE -> "mind_eye.png";
        case PEERLESS_CHALLENGE -> "peerless_challenge.png";
    }); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.MIND_EYE) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.mind_eye.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_MIND_EYE.get(), 400, 0));
            return;
        }
        LivingEntity target = findTarget(player, 14.0D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill." + (art == Art.SKY_CLEAVING_SLASH ? "sky_cleaving_slash" : "peerless_challenge") + ".use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        if (art == Art.SKY_CLEAVING_SLASH) {
            performSkyCleavingSlash(player, target, stats);
        } else {
            playAnimation(target, 33);
            playSound(target, BlackSouls.SAINT7_EVENT.get(), 1.4F);
            target.addEffect(new MobEffectInstance(BlackSouls.BUFF_EXPOSED_WEAKNESS.get(), 1000, 0));
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_NATURAL_RECOVERY.get(), 1000, 0));
            StatEventHandler.applyStats(player);
            StatEventHandler.syncToClient(player);
        }
    }

    public static void playRapierEffects(ServerPlayer player, LivingEntity target) {
        com.BlackSouls.BlackSoulsMod.network.NetworkHandler.sendToAllAround(
                new com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim(548, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()), target
        );
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), BlackSouls.WIND7_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F);
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), BlackSouls.SLASH11_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), BlackSouls.ICE4_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.3F);
    }

    private void performSkyCleavingSlash(ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        playAnimation(target, 548);
        playSound(target, BlackSouls.WIND7_EVENT.get(), 0.5F);
        playSound(target, BlackSouls.SLASH11_EVENT.get(), 0.8F);
        playSound(target, BlackSouls.ICE4_EVENT.get(), 1.3F);
        applyFormulaHit(player, target, stats, 4.0D, 2.0D, 0.20D, true, true, 0.0D);
    }
}
