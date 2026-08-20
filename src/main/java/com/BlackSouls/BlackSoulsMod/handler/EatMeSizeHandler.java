package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundPlayerSizePacket;
import com.BlackSouls.BlackSoulsMod.util.EatMeSizeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class EatMeSizeHandler {
    private EatMeSizeHandler() {
    }

    @SubscribeEvent
    public static void onPlayerSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && EatMeSizeManager.isSmall(player)) {
            event.setNewSize(EatMeSizeManager.SMALL_DIMENSIONS, false);
            event.setNewEyeHeight(EatMeSizeManager.SMALL_EYE_HEIGHT);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.refreshDimensions();
            EatMeSizeManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.refreshDimensions();
            EatMeSizeManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.refreshDimensions();
            EatMeSizeManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        EatMeSizeManager.setSmall(event.getEntity(), !event.isWasDeath() && EatMeSizeManager.isSmall(event.getOriginal()));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof Player target) {
            NetworkHandler.sendToPlayer(new ClientboundPlayerSizePacket(target.getId(), EatMeSizeManager.isSmall(target)), observer);
        }
    }
}
