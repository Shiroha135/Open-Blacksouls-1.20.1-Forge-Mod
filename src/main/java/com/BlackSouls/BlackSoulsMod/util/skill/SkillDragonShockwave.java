package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("resource")
public class SkillDragonShockwave extends WeaponSkill {

    @Override
    public String getSkillId() { return "bs2_skill_dragon_shockwave"; }

    @Override
    public float getManaCost() { return 15.0f; } 

    @Override
    public int getBaseCooldownTicks() { return 1000; } 

    @Override
    public String getTranslationKey() { return "skill.blacksouls.dragon_shockwave.name"; }

    @Override
    public ChatFormatting getTextColor() { return ChatFormatting.YELLOW; }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        return !mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DRAKE_SWORD.get();
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        double aoeRange = 10.0;
        List<LivingEntity> targets = findAllTargets(player, aoeRange);

        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.dragon_shockwave.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);

        playAnim(player, 175);
        playSkillSound(player, BlackSouls.MONSTER4_EVENT.get(), 1.0f, 1.0f);

        for (LivingEntity target : targets) {
            playAnim(target, 175);
            applyHit(player, target, stats, 4.0, true);

            if (Math.random() < 0.50 && BlackSouls.BUFF_STUN.isPresent()) {
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_STUN.get(), 60, 0));
            }
        }
    }

    private void playSkillSound(LivingEntity source, SoundEvent sound, float volume, float pitch) {
        source.level().playSound(null, source.getX(), source.getY(), source.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void playAnim(LivingEntity target, int animId) {
        PacketPlayAnim animPacket = new PacketPlayAnim(animId, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
        NetworkHandler.sendToAllAround(animPacket, target);
    }

    private void applyHit(ServerPlayer player, LivingEntity target, BSPlayerStats stats, double atkMultiplier, boolean isSureHit) {
        if (target == null || target.isRemoved() || target.getHealth() <= 0) return;

        double bDef = StatEventHandler.getRpgPhysicalDefense(target);

        double rawDamage = (stats.attack * atkMultiplier) - (bDef * 2.0);
        if (rawDamage < 1.0) rawDamage = 1.0;

        double variance = 0.75 + (Math.random() * 0.50);
        rawDamage *= variance;

        float finalDamage = StatEventHandler.rollSkillCrit(player, (float) rawDamage);
        target.invulnerableTime = 0;

        DamageSource source = isSureHit ? player.damageSources().indirectMagic(player, player) : player.damageSources().playerAttack(player);
        target.hurt(source, finalDamage);
    }

    private List<LivingEntity> findAllTargets(Player player, double range) {
        List<LivingEntity> targets = new ArrayList<>();
        List<Entity> list = player.level().getEntities(player, player.getBoundingBox().inflate(range));
        for (Entity e : list) {
            if (e instanceof LivingEntity le && le.isAlive() && !le.isSpectator()) {
                if (player.distanceToSqr(le) <= range * range) {
                    targets.add(le);
                }
            }
        }
        return targets;
    }
}
