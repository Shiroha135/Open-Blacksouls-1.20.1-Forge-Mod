package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.util.BSMobStatManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

@SuppressWarnings("removal")
public class SkillMaceArt extends AbstractOriginalWeaponSkill {
    public enum Art { MANA_RECOVERY, MANA_BURN }
    private final Art art;

    public SkillMaceArt(Art art) {
        super(Family.MACE, art == Art.MANA_BURN);
        this.art = art;
    }

    @Override public String getSkillId() { return art == Art.MANA_RECOVERY ? "bs2_skill_mana_recovery" : "bs2_skill_mana_burn"; }
    @Override public float getManaCost() { return art == Art.MANA_RECOVERY ? 0.0F : 30.0F; }
    @Override public int getBaseCooldownTicks() { return 1000; }
    @Override public String getTranslationKey() { return "skill.blacksouls." + getSkillId() + ".name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.AQUA; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID,
            art == Art.MANA_RECOVERY ? "textures/gui/skills/mana_recovery.png" : "textures/gui/skills/mana_burn.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        if (art == Art.MANA_RECOVERY) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.mana_recovery.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
            player.swing(InteractionHand.MAIN_HAND, true);
            playAnimation(player, 228);
            playSound(player, BlackSouls.MAGIC2_EVENT.get(), 1.5F);
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_MANA_REGEN.get(), 800, 0));
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(7, () -> playSound(player, BlackSouls.SAINT6_EVENT.get(), 1.2F)));
            return;
        }

        List<LivingEntity> targets = findTargets(player, 12.0D, Integer.MAX_VALUE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.mana_burn.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        targets.forEach(target -> playAnimation(target, 229));
        playSound(player, BlackSouls.FOG2_EVENT.get(), 1.0F);
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(10, () -> playSound(player, BlackSouls.ABSORB1_EVENT.get(), 1.0F)));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(17, () -> {
            playSound(player, BlackSouls.RAISE3_EVENT.get(), 1.0F);
            playSound(player, BlackSouls.SILENCE_EVENT.get(), 1.0F);
        }));
        player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(20, () -> {
            playSound(player, BlackSouls.EXPLOSION2_EVENT.get(), 0.8F);
            playSound(player, BlackSouls.BATTLE3_EVENT.get(), 1.0F);
            for (LivingEntity target : targets) burnMana(player, target, stats);
        }));
    }

    private void burnMana(ServerPlayer caster, LivingEntity target, BSPlayerStats stats) {
        double amount = stats.magicAttack * 8.0D - StatEventHandler.getRpgMagicDefense(target) * 2.0D;
        amount = Math.max(1.0D, amount * (0.8D + Math.random() * 0.4D));
        if (caster.getRandom().nextDouble() * 100.0D < stats.critRate) {
            amount *= 3.0D;
            caster.sendSystemMessage(Component.translatable("message.blacksouls.combat.crit").withStyle(ChatFormatting.DARK_RED));
        }
        if (target instanceof ServerPlayer targetPlayer) {
            BSPlayerStats targetStats = targetPlayer.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (targetStats != null) {
                double burned = Math.min(targetStats.mp, amount);
                targetStats.mp -= burned;
                StatEventHandler.syncToClient(targetPlayer);
                caster.sendSystemMessage(Component.translatable("message.blacksouls.skill.mana_burn.result", targetPlayer.getName().getString(), (int) burned).withStyle(ChatFormatting.AQUA));
            }
            return;
        }
        BSMobStatManager.MobStats mobStats = BSMobStatManager.getStats(target);
        if (mobStats.maxMana <= 0.0D) return;
        String key = "bs2_mob_mana";
        double current = target.getPersistentData().contains(key) ? target.getPersistentData().getDouble(key) : mobStats.maxMana;
        target.getPersistentData().putDouble(key, Math.max(0.0D, current - amount));
    }
}
