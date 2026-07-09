package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PotionStun extends BlackSoulsMobEffect {
    public PotionStun() {
        super(MobEffectCategory.HARMFUL, 0xFFD700);
        this.addMultiplyTotalModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", -10.0D);
    }
}
