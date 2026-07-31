package com.BlackSouls.BlackSoulsMod.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void blacksouls$startUnlit(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> callback
    ) {
        BlockState state = callback.getReturnValue();
        if (state != null && state.hasProperty(CampfireBlock.LIT)) {
            callback.setReturnValue(state.setValue(CampfireBlock.LIT, false));
        }
    }
}
