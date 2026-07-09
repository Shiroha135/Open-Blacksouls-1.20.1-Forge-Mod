package com.BlackSouls.BlackSoulsMod.sound;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BSSoundRegistry {
    
    public static final DeferredRegister<SoundEvent> SOUNDS = 
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BlackSouls.MODID);

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}