package com.BlackSouls.BlackSoulsMod.mixin.client;

import com.BlackSouls.BlackSoulsMod.client.gui.GuiTurnBattle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceClientMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void blackSouls$freezeClientBattleEffect(LivingEntity entity, Runnable onExpiration,
                                                      CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity == minecraft.player && minecraft.screen instanceof GuiTurnBattle) {
            MobEffectInstance self = (MobEffectInstance) (Object) this;
            cir.setReturnValue(self.isInfiniteDuration() || self.getDuration() > 0);
        }
    }
}
