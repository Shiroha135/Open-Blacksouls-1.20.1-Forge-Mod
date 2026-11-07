package yuki135.blacksouls.util;

import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.story.OriginalOpeningProfile;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerPlayer;

public final class OriginalOpeningCareers {
    private OriginalOpeningCareers() {
    }

    public static int selectedOrigin(ServerPlayer player) {
        if (player == null) return 0;
        return player.getCapability(BSPlayerStats.CAPABILITY).map(stats -> stats.openingProfile().realOriginId()).orElse(0);
    }

    public static int actorId(ServerPlayer player) {
        if (player == null) return 0;
        var stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) return 2;
        var profile = stats.openingProfile();
        if (profile.readState() != OriginalOpeningProfile.ReadState.REAL) return 2;
        return profile.realOriginId() == 0 ? 1 : OriginalOpeningData.get(profile.realOriginId()).actorId();
    }

    public static int classId(ServerPlayer player) {
        int origin = selectedOrigin(player);
        return origin == 0 ? 1 : OriginalOpeningData.get(origin).classId();
    }

    public static OptionalLong variable(ServerPlayer player, int id) {
        if (player == null || id != 6 && id != 8 && id != 14) return OptionalLong.empty();
        var stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null || stats.openingProfile().readState() != OriginalOpeningProfile.ReadState.REAL) return OptionalLong.empty();
        var profile = stats.openingProfile();
        return OptionalLong.of(switch (id) {
            case 6 -> profile.realOriginId();
            case 8 -> profile.realRelationshipId();
            case 14 -> profile.realOriginId() == 0 ? 0 : OriginalOpeningData.get(profile.realOriginId()).actorId();
            default -> throw new IllegalStateException();
        });
    }

    public static boolean writeVariable(ServerPlayer player, int id, long value) {
        var owned = variable(player, id);
        if (owned.isEmpty()) return false;
        if (owned.getAsLong() != value) throw new UnsupportedOperationException("opening_choice_requires_owner_service:" + id);
        return true;
    }
}
