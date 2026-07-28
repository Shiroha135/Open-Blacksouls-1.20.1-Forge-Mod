package com.BlackSouls.BlackSoulsMod.mixin;

import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceBattleMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void blackSouls$freezeBattleEffect(LivingEntity entity, Runnable onExpiration,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (TurnBattleManager.shouldFreezeEffectTick(entity)) {
            MobEffectInstance self = (MobEffectInstance) (Object) this;
            cir.setReturnValue(self.isInfiniteDuration() || self.getDuration() > 0);
        }
    }
}
