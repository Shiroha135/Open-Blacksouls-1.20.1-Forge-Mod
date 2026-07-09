package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PotionHollowed extends BlackSoulsMobEffect {
    public PotionHollowed() {
        super(MobEffectCategory.HARMFUL, 0x333333);
        this.addMultiplyTotalModifier(Attributes.MAX_HEALTH, "5D6F0BA2-1186-46AC-B896-C61C5CEE99CC", -0.20D);
    }
}
