package yuki135.blacksouls.network.packets;

import yuki135.blacksouls.util.DevBonfireService;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundDevBonfirePacket {
    public enum Action {
        LIST,
        REFRESH,
        TELEPORT
    }

    private final Action action;
    private final GlobalPos destination;

    public ServerboundDevBonfirePacket(boolean refresh) {
        this.action = refresh ? Action.REFRESH : Action.LIST;
        this.destination = null;
    }

    public ServerboundDevBonfirePacket(GlobalPos destination) {
        this.action = Action.TELEPORT;
        this.destination = destination;
    }

    public ServerboundDevBonfirePacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(Action.class);
        this.destination = this.action == Action.TELEPORT ? buffer.readGlobalPos() : null;
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        if (this.action == Action.TELEPORT) {
            buffer.writeGlobalPos(this.destination);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            var player = context.getSender();
            if (!DevBonfireService.canUse(player)) {
                if (player != null && this.action != Action.TELEPORT) {
                    yuki135.blacksouls.network.NetworkHandler.sendToPlayer(
                            new ClientboundDevBonfiresPacket(java.util.List.of(), false), player);
                }
                return;
            }
            if (this.action == Action.TELEPORT) {
                DevBonfireService.teleport(player, this.destination);
            } else {
                DevBonfireService.request(player, this.action == Action.REFRESH);
            }
        });
    }
}
