package com.BlackSouls.BlackSoulsMod.mixin;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemStackMaxStackMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void blackSouls$raiseDefaultStackLimit(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 64) {
            cir.setReturnValue(99);
        }
    }
}
