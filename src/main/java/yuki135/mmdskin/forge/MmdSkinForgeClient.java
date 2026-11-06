package yuki135.mmdskin.forge;

import yuki135.mmdskin.MmdSkin;
import yuki135.mmdskin.MmdSkinClient;
import yuki135.mmdskin.forge.config.MmdSkinConfig;
import yuki135.mmdskin.forge.entity.MobReplacementRenderEventHandler;
import yuki135.mmdskin.forge.maid.MaidRenderEventHandler;
import yuki135.mmdskin.forge.register.MmdSkinRegisterClient;
import yuki135.mmdskin.render.bootstrap.ClientRenderRuntime;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Forge 客户端初始化入口。 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = MmdSkin.MOD_ID)
public class MmdSkinForgeClient {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        MmdSkinRegisterClient.onRegisterKeyMappings(event);
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        MmdSkinRegisterClient.onRegisterEntityRenderers(event);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        MmdSkinConfig.init();
        MmdSkinClient.initClient();
        MmdSkinRegisterClient.Register();
        ClientRenderRuntime.get().renderBackendSettings().setShaderEnabled(yuki135.mmdskin.config.ConfigManager.isMMDShaderEnabled());
        MinecraftForge.EVENT_BUS.register(new MobReplacementRenderEventHandler());
        MinecraftForge.EVENT_BUS.register(new MaidRenderEventHandler());
    }
}
