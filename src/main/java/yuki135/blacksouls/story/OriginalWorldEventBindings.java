package yuki135.blacksouls.story;

import yuki135.blacksouls.compat.scene.OriginalNpcPlacements;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerPlayer;

public final class OriginalWorldEventBindings {
    private OriginalWorldEventBindings() {
    }

    public static List<OriginalNpcPlacements.Site> sites() {
        return Stream.of(OriginalAltarEntry.sites(), OriginalDodoEntry.sites(), OriginalFishingRodEntry.sites())
                .flatMap(List::stream).toList();
    }

    public static boolean isBound(ServerPlayer player, EntityOriginalNpc actor) {
        if (OriginalFishingRodEntry.handles(actor.getNpcId())) return OriginalFishingRodEntry.isBound(player, actor);
        return OriginalDodoEntry.handles(actor.getNpcId()) ? OriginalDodoEntry.isBound(player, actor) : OriginalAltarEntry.isBound(player, actor);
    }

    public static int transfer(ServerPlayer player, EntityOriginalNpc actor, int map, int x, int y, int direction, int fade) {
        if (OriginalFishingRodEntry.handles(actor.getNpcId())) throw new UnsupportedOperationException("fishing_rod_transfer");
        return OriginalDodoEntry.handles(actor.getNpcId()) ? OriginalDodoEntry.transfer(player, actor, map, x, y, direction, fade)
                : OriginalAltarEntry.transfer(player, actor, map, x, y, direction, fade);
    }
}
