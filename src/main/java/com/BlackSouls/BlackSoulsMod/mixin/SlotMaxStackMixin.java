package com.BlackSouls.BlackSoulsMod.mixin;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMaxStackMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void blackSouls$raiseSlotStackLimit(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 64) {
            cir.setReturnValue(99);
        }
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void blackSouls$raiseSlotStackLimitForItem(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 64 && stack.getMaxStackSize() == 99) {
            cir.setReturnValue(99);
        }
    }
}
