package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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
        MobSpawnType spawnType = event.getSpawnType();
        if (event.getLevel().getLevel().dimension().equals(LIBRARY_DIM)
                && (spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.CHUNK_GENERATION
                || spawnType == MobSpawnType.PATROL
                || spawnType == MobSpawnType.REINFORCEMENT)) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onForbiddenEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getLevel().dimension().equals(LIBRARY_DIM)
                && (event.getEntity().getType() == EntityType.BAT
                || event.getEntity().getType() == EntityType.GLOW_SQUID)) {
            event.setCanceled(true);
        }
    }
}
