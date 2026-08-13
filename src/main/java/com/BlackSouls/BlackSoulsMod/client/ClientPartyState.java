package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundPartyStatePacket;
import java.util.List;

public final class ClientPartyState {
    private static List<ClientboundPartyStatePacket.Member> members = List.of();

    public static List<ClientboundPartyStatePacket.Member> getMembers() {
        return members;
    }

    public static void setMembers(List<ClientboundPartyStatePacket.Member> value) {
        members = List.copyOf(value);
    }

    private ClientPartyState() {}
}
