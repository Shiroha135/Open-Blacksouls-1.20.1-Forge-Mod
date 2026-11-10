package yuki135.blacksouls.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.client.gui.GuiOriginalNpcDialogue;
import yuki135.blacksouls.client.render.TextBannerRenderer;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPacket;
import yuki135.blacksouls.network.packets.ServerboundOriginalNpcPacket;
import yuki135.blacksouls.story.OriginalNpcInterpreter;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalNpcVictoryBanner {
    private static ClientboundOriginalNpcPacket pending;

    private OriginalNpcVictoryBanner() {
    }

    public static boolean show(ClientboundOriginalNpcPacket packet) {
        if (packet.type() != OriginalNpcInterpreter.Type.TEXT || !packet.portrait().isEmpty()) return false;
        String text = packet.text().replaceAll("\\\\C\\[\\d+\\]", "")
                .replace("\\{", "").replace("\\}", "").strip().replaceAll("\\s+", " ");
        if (!text.equals("VICTORY ACHIEVED") && !text.equals("NIGHTMARE SPIRIT DESTROYED")
                && !text.equals("MADNES SPIRIT DESTROYED")) return false;
        if (pending != null && pending.entityId() == packet.entityId()
                && pending.sessionId().equals(packet.sessionId()) && pending.revision() == packet.revision()) return true;
        pending = packet;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GuiOriginalNpcDialogue) minecraft.setScreen(null);
        TextBannerRenderer.showCentered(Component.literal(text), 0xFFFFA0, 80, null);
        return true;
    }

    public static void clear() {
        if (pending != null) TextBannerRenderer.hide();
        pending = null;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending == null || TextBannerRenderer.isCenteredBannerActive()) return;
        ClientboundOriginalNpcPacket packet = pending;
        pending = null;
        if (Minecraft.getInstance().getConnection() != null) {
            NetworkHandler.sendToServer(new ServerboundOriginalNpcPacket(packet.entityId(), packet.sessionId(), packet.revision(), -1));
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
