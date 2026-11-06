package yuki135.mmdskin.forge;

import yuki135.mmdskin.MmdSkin;
import yuki135.mmdskin.MmdSkinClient;
import yuki135.mmdskin.forge.config.MmdSkinConfig;
import yuki135.mmdskin.render.bootstrap.ClientRenderRuntime;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = MmdSkin.MOD_ID)
public final class MmdSkinForgeClient {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MmdSkinConfig.init();
            MmdSkinClient.initClient();
        });
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MmdSkin.MOD_ID)
    public static final class RuntimeEvents {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) ClientRenderRuntime.get().modelRepository().tick();
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientRenderRuntime.get().modelRepository().reloadAll();
        }
    }
}
