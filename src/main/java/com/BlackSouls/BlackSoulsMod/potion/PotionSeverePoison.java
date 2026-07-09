package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;

public class PotionSeverePoison extends BlackSoulsPercentageDamageEffect {
    public PotionSeverePoison() {
        super(MobEffectCategory.HARMFUL, 0x4A005E, 0.015F, 40);
    }
}
