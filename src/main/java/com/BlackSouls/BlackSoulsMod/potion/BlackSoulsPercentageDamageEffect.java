package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BlackSoulsPercentageDamageEffect extends BlackSoulsMobEffect {
    private final float maxHealthDamage;
    private final int tickInterval;

    protected BlackSoulsPercentageDamageEffect(MobEffectCategory category, int color, float maxHealthDamage, int tickInterval) {
        super(category, color);
        this.maxHealthDamage = maxHealthDamage;
        this.tickInterval = tickInterval;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        float damage = Math.max(1.0F, entity.getMaxHealth() * this.maxHealthDamage);
        entity.hurt(entity.damageSources().magic(), damage * (amplifier + 1));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % this.tickInterval == 0;
    }
}
