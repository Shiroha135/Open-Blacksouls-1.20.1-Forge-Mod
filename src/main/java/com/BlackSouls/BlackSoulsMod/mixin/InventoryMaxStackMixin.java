package com.BlackSouls.BlackSoulsMod.mixin;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Inventory.class)
public abstract class InventoryMaxStackMixin {

    public int getMaxStackSize() {
        return 99;
    }
}
