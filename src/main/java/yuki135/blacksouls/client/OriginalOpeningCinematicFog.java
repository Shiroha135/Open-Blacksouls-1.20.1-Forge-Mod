package yuki135.blacksouls.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.story.OriginalOpeningScenes;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT)
public final class OriginalOpeningCinematicFog {
    private static boolean stopped;
    private OriginalOpeningCinematicFog() {
    }
    public static void setStopped(boolean value) { stopped=value; }
    public static boolean stopped() { return stopped; }

    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void renderFog(ViewportEvent.RenderFog event) {
        var minecraft=Minecraft.getInstance();
        if(!stopped||minecraft.level==null||!OriginalOpeningScenes.DIMENSION.equals(minecraft.level.dimension())||event.getType()!=FogType.NONE)return;
        float far=Math.max(16,minecraft.options.renderDistance().get()*16F);
        event.setNearPlaneDistance(far);event.setFarPlaneDistance(far+16);event.setFogShape(FogShape.CYLINDER);event.setCanceled(true);
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { stopped=false; }
}
