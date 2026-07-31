package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundStoryNamePacket;
import com.BlackSouls.BlackSoulsMod.util.StoryNameData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class StoryNameHandler {
    private static final long NEW_WORLD_TICK_LIMIT = 1200L;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!StoryNameData.hasStarted(player)) {
            long gameTime = player.getServer().overworld().getGameTime();
            if (gameTime <= NEW_WORLD_TICK_LIMIT) {
                StoryNameData.start(player);
            } else {
                StoryNameData.confirm(player, player.getGameProfile().getName());
            }
        }

        boolean needsChoice = !StoryNameData.isConfirmed(player);
        String storyName = needsChoice ? "" : StoryNameData.get(player);
        NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(needsChoice, storyName), player);
    }

    private StoryNameHandler() {
    }
}
