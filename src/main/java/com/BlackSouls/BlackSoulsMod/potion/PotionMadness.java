package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PotionMadness extends BlackSoulsMobEffect {
    public PotionMadness() {
        super(MobEffectCategory.HARMFUL, 0xFFD700);
        this.addMultiplyTotalModifier(Attributes.ATTACK_DAMAGE, "49455A49-7E1A-4563-8957-69502787C171", 1.0D);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() > 1.0F) {
            entity.setHealth(Math.max(1.0F, entity.getHealth() - entity.getMaxHealth() * 0.50F));
        }
    }
}
