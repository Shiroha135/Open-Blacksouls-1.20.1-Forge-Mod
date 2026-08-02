package com.BlackSouls.BlackSoulsMod.mixin.client;

import com.BlackSouls.BlackSoulsMod.client.WindowBranding;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftWindowTitleMixin {

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void blacksouls$useOriginalWindowTitle(CallbackInfoReturnable<String> cir) {
        if (WindowBranding.isEnabled()) {
            cir.setReturnValue(WindowBranding.TITLE);
        }
    }
}
