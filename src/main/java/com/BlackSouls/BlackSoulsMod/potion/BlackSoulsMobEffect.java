package com.BlackSouls.BlackSoulsMod.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class BlackSoulsMobEffect extends MobEffect {
    protected BlackSoulsMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    protected void addMultiplyTotalModifier(Attribute attribute, String uuid, double amount) {
        this.addAttributeModifier(attribute, uuid, amount, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
