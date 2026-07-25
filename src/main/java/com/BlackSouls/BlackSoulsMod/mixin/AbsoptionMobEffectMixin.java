package com.BlackSouls.BlackSoulsMod.mixin;

import com.BlackSouls.BlackSoulsMod.util.VanillaHealthScaling;
import net.minecraft.world.effect.AbsoptionMobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbsoptionMobEffect.class)
public abstract class AbsoptionMobEffectMixin {
    @Redirect(
            method = {"addAttributeModifiers", "removeAttributeModifiers"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V")
    )
    private void blacksouls$scaleAbsorption(LivingEntity entity, float targetAmount) {
        if (!(entity instanceof Player)) {
            entity.setAbsorptionAmount(targetAmount);
            return;
        }
        float current = entity.getAbsorptionAmount();
        float delta = targetAmount - current;
        entity.setAbsorptionAmount(Math.max(0.0F, current + delta * VanillaHealthScaling.getScale(entity)));
    }
}
