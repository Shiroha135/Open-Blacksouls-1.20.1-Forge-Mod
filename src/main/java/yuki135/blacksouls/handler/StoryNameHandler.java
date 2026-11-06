package yuki135.blacksouls.handler;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundStoryNamePacket;
import yuki135.blacksouls.util.StoryNameData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class StoryNameHandler {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        OriginalNpcProgress.get(player.server).bootstrapNameWindow();

        if (!StoryNameData.hasStarted(player)) {
            StoryNameData.start(player);
        }

        if (StoryNameData.isConfirmed(player)
                && "part".equalsIgnoreCase(StoryNameData.get(player))
                && player.getGameProfile().getName().length() > StoryNameData.MAX_LENGTH) {
            StoryNameData.confirmGameName(player);
        }

        boolean needsChoice = !StoryNameData.isConfirmed(player);
        String storyName = needsChoice ? "" : StoryNameData.get(player);
        NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(needsChoice, storyName), player);
    }

    private StoryNameHandler() {
    }
}
