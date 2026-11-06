package yuki135.blacksouls.client;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.story.OriginalNonExplicitScenes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalSceneOmissionController {
    private static int remaining;

    public static void begin() {
        remaining = OriginalNonExplicitScenes.FADE_TICKS;
    }

    public static boolean active() {
        return remaining > 0;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !minecraft.player.isAlive()) remaining = 0;
        else if (!minecraft.isPaused() && remaining > 0) remaining--;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        remaining = 0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGui(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) draw(event.getGuiGraphics());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreen(ScreenEvent.Render.Post event) {
        draw(event.getGuiGraphics());
    }

    private static void draw(GuiGraphics graphics) {
        if (!active()) return;
        float opacity = Math.min(1.0F, Math.min((OriginalNonExplicitScenes.FADE_TICKS - remaining + 1) / 4.0F, remaining / 6.0F));
        Minecraft minecraft = Minecraft.getInstance();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        graphics.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(),
                Math.round(opacity * 255.0F) << 24);
        graphics.pose().popPose();
    }

    private OriginalSceneOmissionController() {
    }
}
