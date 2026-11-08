package yuki135.blacksouls.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="blacksouls",value=Dist.CLIENT)
public final class ClientAvatarEvents {
    private ClientAvatarEvents() { }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {ClientSkillInfo.clearAvatarState();}
}
