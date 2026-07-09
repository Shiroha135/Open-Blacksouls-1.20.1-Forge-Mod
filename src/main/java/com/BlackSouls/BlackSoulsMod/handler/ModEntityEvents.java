package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(BSEntityRegistry.NODEN.get(), EntityNoden.createAttributes().build());
        event.put(BSEntityRegistry.HAIL_CAESAR.get(), EntityHailCaesar.createAttributes().build());
        event.put(BSEntityRegistry.HELL_PRINCE.get(), EntityHellPrince.createAttributes().build());
        event.put(BSEntityRegistry.TEST_DUMMY.get(), EntityTestDummy.createAttributes().build());
    }
}
