package com.BlackSouls.BlackSoulsMod.mixin;

import com.BlackSouls.BlackSoulsMod.util.VanillaHealthScaling;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobEffect.class)
public abstract class MobEffectMixin {
    @Redirect(
            method = {"applyEffectTick", "applyInstantenousEffect"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V")
    )
    private void blacksouls$scaleVanillaEffectHealing(LivingEntity entity, float amount) {
        entity.heal(VanillaHealthScaling.scaleVanillaHealing(entity, amount));
    }
}
