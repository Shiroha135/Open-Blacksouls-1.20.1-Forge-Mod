package com.BlackSouls.BlackSoulsMod.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;

import java.util.function.Supplier;

public class ItemModRecord extends RecordItem {
    
    public ItemModRecord(int comparatorValue, Supplier<SoundEvent> soundSupplier, Properties properties, int lengthInSeconds) {
        super(comparatorValue, soundSupplier, properties, lengthInSeconds * 20);
    }
}