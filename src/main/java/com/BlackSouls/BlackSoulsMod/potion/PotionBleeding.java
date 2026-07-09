package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;

public class PotionBleeding extends BlackSoulsPercentageDamageEffect {
    public PotionBleeding() {
        super(MobEffectCategory.HARMFUL, 0x8A0303, 0.015F, 40);
    }
}
