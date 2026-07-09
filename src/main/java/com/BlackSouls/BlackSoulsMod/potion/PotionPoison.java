package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;

public class PotionPoison extends BlackSoulsPercentageDamageEffect {
    public PotionPoison() {
        super(MobEffectCategory.HARMFUL, 0x8B008B, 0.005F, 40);
    }
}
