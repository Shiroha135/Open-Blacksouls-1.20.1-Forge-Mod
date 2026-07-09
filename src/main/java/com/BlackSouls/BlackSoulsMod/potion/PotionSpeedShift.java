package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PotionSpeedShift extends BlackSoulsMobEffect {
    public PotionSpeedShift(MobEffectCategory category, int color, String uuid, double amount) {
        super(category, color);
        this.addMultiplyTotalModifier(Attributes.MOVEMENT_SPEED, uuid, amount);
    }
}
