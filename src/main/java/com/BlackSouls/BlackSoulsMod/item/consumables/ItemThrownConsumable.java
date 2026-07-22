package com.BlackSouls.BlackSoulsMod.item.consumables;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

public class ItemThrownConsumable extends ItemThrownBladeBase {
    public ItemThrownConsumable(Properties properties, int mode, boolean sureHit, int animationId, RegistryObject<SoundEvent> throwSound) {
        super(properties, mode, sureHit, 0, animationId, throwSound, null, 0);
    }
}
