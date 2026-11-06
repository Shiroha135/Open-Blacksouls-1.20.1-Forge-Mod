package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalNpcPlacements;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerPlayer;

public final class OriginalWorldEventBindings {
    private OriginalWorldEventBindings() {
    }

    public static List<OriginalNpcPlacements.Site> sites() {
        return Stream.concat(OriginalAltarEntry.sites().stream(), OriginalDodoEntry.sites().stream()).toList();
    }

    public static boolean isBound(ServerPlayer player, EntityOriginalNpc actor) {
        return OriginalDodoEntry.handles(actor.getNpcId()) ? OriginalDodoEntry.isBound(player, actor) : OriginalAltarEntry.isBound(player, actor);
    }

    public static int transfer(ServerPlayer player, EntityOriginalNpc actor, int map, int x, int y, int direction, int fade) {
        return OriginalDodoEntry.handles(actor.getNpcId()) ? OriginalDodoEntry.transfer(player, actor, map, x, y, direction, fade)
                : OriginalAltarEntry.transfer(player, actor, map, x, y, direction, fade);
    }
}
