package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundPlayerSizePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;

public final class EatMeSizeManager {
    public static final String SMALL_TAG = "blacksouls_eat_me_small";
    public static final EntityDimensions SMALL_DIMENSIONS = EntityDimensions.scalable(0.45F, 0.45F);
    public static final float SMALL_EYE_HEIGHT = 0.36F;

    private EatMeSizeManager() {
    }

    public static boolean isSmall(Player player) {
        return player.getPersistentData().getBoolean(SMALL_TAG);
    }

    public static void setSmall(Player player, boolean small) {
        if (small) {
            player.getPersistentData().putBoolean(SMALL_TAG, true);
        } else {
            player.getPersistentData().remove(SMALL_TAG);
        }
        player.refreshDimensions();
    }

    public static void sync(ServerPlayer player) {
        NetworkHandler.sendToAllAround(new ClientboundPlayerSizePacket(player.getId(), isSmall(player)), player);
    }
}
