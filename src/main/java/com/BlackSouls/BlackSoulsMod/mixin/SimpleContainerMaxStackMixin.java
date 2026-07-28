package com.BlackSouls.BlackSoulsMod.mixin;

import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SimpleContainer.class)
public abstract class SimpleContainerMaxStackMixin {

    public int getMaxStackSize() {
        return 99;
    }
}
