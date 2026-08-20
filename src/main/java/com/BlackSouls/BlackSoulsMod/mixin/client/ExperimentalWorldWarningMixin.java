package com.BlackSouls.BlackSoulsMod.mixin.client;

import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class ExperimentalWorldWarningMixin {

    @Inject(method = "createWorldConfirmationScreen", at = @At("HEAD"), cancellable = true)
    private static void blacksouls$confirmBundledDimension(Runnable confirmedWorldLoad, CallbackInfo ci) {
        confirmedWorldLoad.run();
        ci.cancel();
    }
}
