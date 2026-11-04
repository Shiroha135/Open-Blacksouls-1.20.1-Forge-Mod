package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.capability.OriginalNpcProgress;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPresentationPacket;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class OriginalBattleMusicService {
    private OriginalBattleMusicService() {
    }

    public static void set(ServerPlayer player, JsonObject rawBgm) {
        ClientboundOriginalNpcPresentationPacket validated = OriginalNpcPresentationService.createPacket(
                player, player, 241, List.of(rawBgm));
        OriginalNpcProgress.get(player.server).setBattleMusic(new OriginalBattleMusic(true,
                validated.sound(), validated.values().get(0), validated.values().get(1)));
    }

    public static OriginalBattleMusic current(ServerPlayer player) {
        return OriginalNpcProgress.get(player.server).battleMusic();
    }
}
