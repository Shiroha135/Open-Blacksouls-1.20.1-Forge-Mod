package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

abstract class AbstractWeaponCombatSkill extends WeaponSkill {
    private static final ThreadLocal<LivingEntity> PUPPET_TARGET = new ThreadLocal<>();

    static void setPuppetTarget(LivingEntity target) {
        PUPPET_TARGET.set(target);
    }

    static void clearPuppetTarget() {
        PUPPET_TARGET.remove();
    }

    protected LivingEntity findTarget(Player player, double range) {
        LivingEntity forcedTarget = PUPPET_TARGET.get();
        if (forcedTarget != null && forcedTarget.isAlive() && !forcedTarget.isSpectator()
                && player.distanceToSqr(forcedTarget) <= range * range) {
            return forcedTarget;
        }
        LivingEntity target = null;
        double closest = Double.MAX_VALUE;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 targetVec = eyePos.add(player.getLookAngle().scale(range));
        List<Entity> entities = player.level().getEntities(player, player.getBoundingBox().inflate(range));

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
                continue;
            }
            AABB hitBox = entity.getBoundingBox().inflate(0.5D);
            if (hitBox.contains(eyePos)) {
                return living;
            }
            Optional<Vec3> hit = hitBox.clip(eyePos, targetVec);
            if (hit.isPresent()) {
                double distance = eyePos.distanceToSqr(hit.get());
                if (distance < closest) {
                    closest = distance;
                    target = living;
                }
            }
        }
        return target;
    }

    protected List<LivingEntity> findTargets(Player player, double range, int maximum) {
        LivingEntity forcedTarget = PUPPET_TARGET.get();
        if (forcedTarget != null && forcedTarget.isAlive() && !forcedTarget.isSpectator()
                && player.distanceToSqr(forcedTarget) <= range * range) {
            return List.of(forcedTarget);
        }
        List<LivingEntity> result = new ArrayList<>();
        LivingEntity primary = findTarget(player, range);
        if (primary != null) {
            result.add(primary);
        }

        List<LivingEntity> nearby = new ArrayList<>();
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(range))) {
            if (entity instanceof LivingEntity living
                    && living.isAlive()
                    && !living.isSpectator()
                    && living != primary
                    && player.distanceToSqr(living) <= range * range) {
                nearby.add(living);
            }
        }
        nearby.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (LivingEntity living : nearby) {
            if (result.size() >= maximum) break;
            result.add(living);
        }
        return result;
    }

    protected void playAnimation(LivingEntity target, int animationId) {
        NetworkHandler.sendToAllAround(new PacketPlayAnim(
                animationId,
                target.getX(),
                target.getY() + target.getBbHeight() / 2.0F,
                target.getZ()
        ), target);
    }

    protected void playSound(LivingEntity target, SoundEvent sound, float pitch) {
        target.level().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                sound,
                SoundSource.PLAYERS,
                1.0F,
                pitch
        );
    }

    protected boolean applyFormulaHit(ServerPlayer player, LivingEntity target, BSPlayerStats stats,
                                      double attackMultiplier, double defenseMultiplier, double variance,
                                      boolean canCrit, boolean sureHit, double instantDeathRate) {
        if (target == null || target.isRemoved() || !target.isAlive()) {
            return false;
        }
        double rawDamage = stats.attack * attackMultiplier
                - StatEventHandler.getRpgPhysicalDefense(target) * defenseMultiplier;
        if (variance > 0.0D) {
            rawDamage *= (1.0D - variance) + Math.random() * variance * 2.0D;
        }
        return applyRawHit(player, target, rawDamage, canCrit, sureHit, instantDeathRate);
    }

    protected boolean applyRawHit(ServerPlayer player, LivingEntity target, double rawDamage,
                                  boolean canCrit, boolean sureHit, double instantDeathRate) {
        if (target == null || target.isRemoved() || !target.isAlive()) {
            return false;
        }
        float damage = (float) Math.max(1.0D, rawDamage);
        if (canCrit) {
            damage = StatEventHandler.rollSkillCrit(player, damage);
        }
        return StatEventHandler.hurtWithSkillDamage(player, target, damage, sureHit, instantDeathRate);
    }
}
