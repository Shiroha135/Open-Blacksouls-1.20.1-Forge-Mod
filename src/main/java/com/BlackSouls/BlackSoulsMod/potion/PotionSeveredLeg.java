package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PotionSeveredLeg extends BlackSoulsMobEffect {
    public PotionSeveredLeg() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
        this.addMultiplyTotalModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160892", -1.0D);
        this.addMultiplyTotalModifier(Attributes.ARMOR, "8207DE5E-7CE8-4030-940E-514C1F160891", -0.99D);
    }
}
