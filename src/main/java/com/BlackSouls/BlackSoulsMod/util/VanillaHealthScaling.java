package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class VanillaHealthScaling {
    private static final float VANILLA_MAX_HEALTH = 20.0F;

    public static float getScale(LivingEntity entity) {
        return Math.max(1.0F, entity.getMaxHealth() / VANILLA_MAX_HEALTH);
    }

    public static float scaleVanillaHealing(LivingEntity entity, float amount) {
        return entity instanceof Player ? amount * getScale(entity) : amount;
    }

    public static float scaleVanillaDamage(Player victim, DamageSource source, float amount) {
        return shouldScaleDamage(source, amount) ? amount * getScale(victim) : amount;
    }

    private static boolean shouldScaleDamage(DamageSource source, float amount) {
        String messageId = source.getMsgId();
        if (messageId.startsWith("blacksouls") || messageId.startsWith("bs2_")) {
            return false;
        }

        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if (isBlackSoulsEntity(direct) || isBlackSoulsEntity(attacker)) {
            return false;
        }

        if (attacker instanceof Player player) {
            if (source.is(DamageTypes.PLAYER_ATTACK)) {
                return false;
            }
            if (player.getPersistentData().getBoolean("bs2_precomputed_skill_damage")
                    || player.getPersistentData().getBoolean("bs2_dagger_extra_hit")
                    || player.getPersistentData().getBoolean("bs2_ring_extra_hit")) {
                return false;
            }
            return true;
        }

        if (attacker instanceof LivingEntity livingAttacker) {
            return !BSMobStatManager.hasManagedStats(livingAttacker);
        }

        if (direct != null) {
            return true;
        }

        return source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.DRAGON_BREATH)
                || source.is(DamageTypes.DRY_OUT)
                || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.FREEZE)
                || source.is(DamageTypes.STALAGMITE)
                || source.is(DamageTypes.FALLING_BLOCK)
                || source.is(DamageTypes.FALLING_ANVIL)
                || source.is(DamageTypes.FALLING_STALACTITE)
                || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.BAD_RESPAWN_POINT)
                || source.is(DamageTypes.OUTSIDE_BORDER)
                || source.is(DamageTypes.MAGIC) && amount <= 12.0F;
    }

    private static boolean isBlackSoulsEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        return BlackSouls.MODID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
    }

    private VanillaHealthScaling() {
    }
}
