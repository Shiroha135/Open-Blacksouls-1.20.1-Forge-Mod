package com.BlackSouls.BlackSoulsMod.mixin.client;

import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public abstract class ExperimentalWorldWarningMixin {
    @Unique
    private Runnable blacksouls$pendingConfirmedWorldLoad;

    @Redirect(
            method = "doLoadLevel(Lnet/minecraft/client/gui/screens/Screen;Ljava/lang/String;ZZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/ForgeHooksClient;createWorldConfirmationScreen(Ljava/lang/Runnable;)V",
                    remap = false
            ),
            remap = false
    )
    private void blacksouls$deferBundledDimensionConfirmation(Runnable confirmedWorldLoad) {
        blacksouls$pendingConfirmedWorldLoad = confirmedWorldLoad;
    }

    @Inject(
            method = "doLoadLevel(Lnet/minecraft/client/gui/screens/Screen;Ljava/lang/String;ZZZ)V",
            at = @At("RETURN"),
            remap = false
    )
    private void blacksouls$runConfirmedWorldLoad(CallbackInfo ci) {
        Runnable confirmedWorldLoad = blacksouls$pendingConfirmedWorldLoad;
        blacksouls$pendingConfirmedWorldLoad = null;
        if (confirmedWorldLoad != null) {
            confirmedWorldLoad.run();
        }
    }
}
