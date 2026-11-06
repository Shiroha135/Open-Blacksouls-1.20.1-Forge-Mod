package yuki135.blacksouls.network.packets;

import yuki135.blacksouls.client.render.BonfireEffectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSimpleActionPacket {
    private final Action action;

    public ClientboundSimpleActionPacket(Action action) {
        this.action = action;
    }

    public ClientboundSimpleActionPacket(FriendlyByteBuf buf) {
        this.action = PacketHandlers.readEnum(buf, Action.values());
    }

    public void toBytes(FriendlyByteBuf buf) {
        PacketHandlers.writeEnum(buf, this.action);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> {
            if (this.action != null) {
                ClientHandler.handle(this.action);
            }
        });
    }

    public enum Action {
        OPEN_YOU_DIED_SCREEN,
        SHOW_RETRIEVAL_BANNER,
        SHOW_BONFIRE_LIT
    }

    private static class ClientHandler {
        private static void handle(Action action) {
            Minecraft mc = Minecraft.getInstance();
            switch (action) {
                case OPEN_YOU_DIED_SCREEN -> mc.setScreen(new yuki135.blacksouls.client.gui.GuiYouDied());
                case SHOW_RETRIEVAL_BANNER -> mc.setScreen(new yuki135.blacksouls.client.gui.GuiRetrievalBanner());
                case SHOW_BONFIRE_LIT -> BonfireEffectRenderer.bonfireLitTicks = 80;
            }
        }
    }
}
