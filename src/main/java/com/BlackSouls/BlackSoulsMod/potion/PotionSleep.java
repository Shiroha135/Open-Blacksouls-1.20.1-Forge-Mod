package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PotionSleep extends BlackSoulsMobEffect {
    public PotionSleep() {
        super(MobEffectCategory.HARMFUL, 0x483D8B);
        this.addMultiplyTotalModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160891", -1.0D);
        this.addMultiplyTotalModifier(Attributes.ATTACK_DAMAGE, "22653B89-116E-49DC-9B6B-9971489B5BE5", -1.0D);
    }
}
