package yuki135.blacksouls.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.network.packets.ClientboundBlackAshPacket;

@Mod.EventBusSubscriber(modid="blacksouls", value=Dist.CLIENT)
public final class BlackAshPresentationController {
    private static int phase = -1;
    private static int ticks;
    private static int watchdog;
    private static int readyTicks;
    private static boolean readySent;
    private static ResourceLocation destination;

    private BlackAshPresentationController() { }

    public static void apply(ClientboundBlackAshPacket packet) {
        if (packet.phase() == 2) { clear(); return; }
        phase = packet.phase();
        ticks = 0;
        watchdog = 0;
        readyTicks = 0;
        readySent = false;
        destination = packet.dimension();
        if (phase == 0) Minecraft.getInstance().setScreen(null);
    }

    public static boolean active() { return phase >= 0; }

    private static boolean destinationReady() {
        var minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.player != null && minecraft.screen == null
                && minecraft.level.dimension().location().equals(destination)
                && minecraft.level.hasChunkAt(minecraft.player.blockPosition())
                && minecraft.levelRenderer.isChunkCompiled(minecraft.player.blockPosition());
    }

    public static float black(float partialTicks) {
        float elapsed = ticks + partialTicks;
        if (phase == 0) return Math.max(0, Math.min(1, (elapsed - 20) / 10));
        if (phase == 1) return 1;
        if (phase == 3) {
            if (!destinationReady() || readyTicks < 3) return 1;
            return Math.max(0, 1 - elapsed / 10);
        }
        return 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active()) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !minecraft.player.isAlive() || ++watchdog > 1200) { clear(); return; }
        if (minecraft.isPaused()) return;
        if (phase == 0) ticks++;
        else if (destinationReady()) {
            readyTicks++;
            if (readyTicks >= 3 && phase == 1 && !readySent) {
                readySent = true;
                yuki135.blacksouls.network.NetworkHandler.sendToServer(new yuki135.blacksouls.network.packets.ServerboundBlackAshReadyPacket());
            }
            if (readyTicks >= 3 && phase == 3) ticks++;
        }
        if (phase == 3 && ticks >= 30) clear();
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void render(RenderGuiEvent.Post event) {
        cover(event.getGuiGraphics(), black(event.getPartialTick()), event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight());
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void screen(ScreenEvent.Render.Post event) {
        cover(event.getGuiGraphics(), black(event.getPartialTick()), event.getScreen().width, event.getScreen().height);
    }

    private static void cover(net.minecraft.client.gui.GuiGraphics graphics, float black, int width, int height) {
        if (black <= 0) return;
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 2000);
        graphics.fill(0, 0, width, height, Math.round(black * 255) << 24);
        graphics.flush();
        graphics.pose().popPose();
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        if (!active()) return;
        var input = event.getInput();
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = false; input.down = false; input.left = false; input.right = false;
        input.jumping = false; input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void interact(InputEvent.InteractionKeyMappingTriggered event) {
        if (active()) { event.setCanceled(true); event.setSwingHand(false); }
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) { if (active()) event.setCanceled(true); }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    private static void clear() { phase = -1; ticks = 0; watchdog = 0; readyTicks = 0; destination = null; }
}
