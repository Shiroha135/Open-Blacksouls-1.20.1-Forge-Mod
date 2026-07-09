package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DimensionRuleHandler {

    
    private static final ResourceKey<Level> LIBRARY_DIM = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("blacksouls", "library"));
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().dimension().equals(LIBRARY_DIM)) {
            
            if (!player.isCreative()) {
                event.setCanceled(true);
            }
        }
    }

    
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.level().dimension().equals(LIBRARY_DIM) && !player.isCreative()) {
                event.setCanceled(true);
            }
        }
    }

    
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        
        if (event.getLevel().getLevel().dimension().equals(LIBRARY_DIM)) {
            event.setSpawnCancelled(true);
        }
    }
}