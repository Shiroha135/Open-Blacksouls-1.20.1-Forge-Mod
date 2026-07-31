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
        event.put(BSEntityRegistry.RED_HOOD.get(), EntityRedHood.createAttributes().build());
        event.put(BSEntityRegistry.RABBIT_HOLE_NPC.get(), EntityRabbitHoleNpc.createAttributes().build());
        event.put(BSEntityRegistry.HAIL_CAESAR.get(), EntityHailCaesar.createAttributes().build());
        event.put(BSEntityRegistry.HELL_PRINCE.get(), EntityHellPrince.createAttributes().build());
        event.put(BSEntityRegistry.TEST_DUMMY.get(), EntityTestDummy.createAttributes().build());
        event.put(BSEntityRegistry.MEAT_WALL.get(), EntityMeatWall.createAttributes().build());
        event.put(BSEntityRegistry.CORPSE_EATING_RABBIT.get(), EntityCorpseEatingRabbit.createAttributes().build());
        event.put(BSEntityRegistry.ORIGINAL_ENEMY.get(), EntityOriginalDatabaseEnemy.createAttributes().build());
        event.put(BSEntityRegistry.HEADLESS_UNDEAD.get(),
                EntityOriginalTurnBattleEnemy.createAttributes(
                        EntityOriginalTurnBattleEnemy.Profile.HEADLESS_UNDEAD).build());
        event.put(BSEntityRegistry.CORRUPT_DOG.get(),
                EntityOriginalTurnBattleEnemy.createAttributes(
                        EntityOriginalTurnBattleEnemy.Profile.CORRUPT_DOG).build());
        event.put(BSEntityRegistry.WEREWOLF.get(),
                EntityOriginalTurnBattleEnemy.createAttributes(
                        EntityOriginalTurnBattleEnemy.Profile.WEREWOLF).build());
    }
}
