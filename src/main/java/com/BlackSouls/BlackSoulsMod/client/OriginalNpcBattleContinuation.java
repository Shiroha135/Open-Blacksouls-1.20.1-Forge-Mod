package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundOriginalNpcPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalNpcBattleContinuation {
    private static ClientboundOriginalNpcPacket pending;

    private OriginalNpcBattleContinuation() {
    }

    public static void begin(ClientboundOriginalNpcPacket packet) {
        pending = packet;
    }

    public static void clear(ClientboundOriginalNpcPacket packet) {
        if (pending != null && pending.entityId() == packet.entityId()
                && pending.sessionId().equals(packet.sessionId())) {
            pending = null;
        }
    }

    public static void clear() {
        pending = null;
    }

    public static void finished() {
        ClientboundOriginalNpcPacket packet = pending;
        pending = null;
        if (packet != null) {
            NetworkHandler.sendToServer(new ServerboundOriginalNpcPacket(packet.entityId(), packet.sessionId(),
                    packet.revision(), -4));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        pending = null;
    }
}
