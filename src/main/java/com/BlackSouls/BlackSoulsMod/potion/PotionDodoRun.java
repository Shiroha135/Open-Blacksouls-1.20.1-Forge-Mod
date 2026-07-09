package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PotionDodoRun extends BlackSoulsMobEffect {
    public PotionDodoRun() {
        super(MobEffectCategory.BENEFICIAL, 0x00BFFF);
        this.addMultiplyTotalModifier(Attributes.MOVEMENT_SPEED, "91AEAA56-376B-4498-935B-2F7F68070635", 0.2D);
    }
}
