package yuki135.blacksouls.story;

import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.PacketSyncStats;
import yuki135.blacksouls.util.OriginalSpiritData;
import yuki135.blacksouls.util.OriginalCovenantData;
import yuki135.blacksouls.util.BSOriginalNpcData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

public final class OriginalSpiritService {
    private OriginalSpiritService() {
    }

    public static void beginNodenConversation(ServerPlayer player) {
        BSPlayerStats stats = stats(player);
        initialize(stats);
        if (!stats.originalNodenProgressKnown) {
            stats.originalNodenProgressKnown = true;
            stats.originalNodenPledged = false;
            stats.originalNodenStage = 0;
        }
    }

    public static void restoreLegacyNodenProgress(ServerPlayer player) {
        BSPlayerStats stats = stats(player);
        initialize(stats);
        if (!stats.originalNodenProgressKnown) {
            var progress = OriginalNpcProgress.get(player.server);
            stats.originalNodenProgressKnown = true;
            stats.originalNodenPledged = progress.switchValue(157);
            stats.originalNodenStage = progress.variable(68);
        }
    }

    public static void change(ServerPlayer player, int spiritId, boolean add) {
        if (OriginalSpiritData.get(spiritId) == null) throw new UnsupportedOperationException("unknown_original_spirit:" + spiritId);
        BSPlayerStats stats = stats(player);
        initialize(stats);
        if (add) {
            if (!stats.originalSpirits.contains(spiritId)) stats.originalSpirits.add(spiritId);
        } else {
            stats.originalSpirits.remove(Integer.valueOf(spiritId));
            if (stats.originalSpiritPartner == spiritId) stats.originalSpiritPartner = 0;
        }
        synchronizeLegacyNoden(stats);
        update(player);
    }

    public static void restoreLegacyCovenantProgress(ServerPlayer player, JsonObject snapshot) {
        if (snapshot.has("contextState") && snapshot.getAsJsonObject("contextState").has("personalCovenants")) return;
        BSPlayerStats stats = stats(player);
        for (var covenant : OriginalCovenantData.values()) {
            if (covenant.commonEvent() == 80 || stats.originalCovenants.containsKey(covenant.commonEvent())) continue;
            JsonArray expected = new JsonArray();
            for (var command : BSOriginalNpcData.commonEvent(covenant.commonEvent())) {
                JsonObject row = new JsonObject();
                row.addProperty("code", command.code());
                row.addProperty("indent", command.indent());
                JsonArray parameters = new JsonArray();
                command.parameters().forEach(value -> parameters.add(value.deepCopy()));
                row.add("parameters", parameters);
                expected.add(row);
            }
            for (var frame : snapshot.getAsJsonArray("frames")) {
                if (expected.equals(frame.getAsJsonObject().getAsJsonArray("commands"))) {
                    var progress = OriginalNpcProgress.get(player.server);
                    stats.originalCovenants.put(covenant.commonEvent(), new BSPlayerStats.CovenantProgress(
                            progress.switchValue(covenant.pledgeSwitch()), progress.variable(covenant.stageVariable())));
                    break;
                }
            }
        }
    }

    public static boolean select(ServerPlayer player, int spiritId) {
        if (!player.isAlive() || player.isSpectator() || TurnBattleManager.isInBattle(player)
                || spiritId < 0 || spiritId > 68) return false;
        BSPlayerStats stats = stats(player);
        initialize(stats);
        if (spiritId != 0 && (!stats.originalSpirits.contains(spiritId) || !OriginalSpiritData.supported(spiritId))) return false;
        if (stats.originalSpiritPartner == spiritId && (spiritId != 0 || stats.activeCovenant.isEmpty())) return false;
        stats.originalSpiritPartner = spiritId;
        stats.activeCovenant = spiritId == 0 ? "" : spiritId <= 4 ? "noden" : "spirit:" + spiritId;
        synchronizeLegacyNoden(stats);
        update(player);
        return true;
    }

    public static void synchronizeLegacyNoden(BSPlayerStats stats) {
        int highest = stats.originalSpirits.stream().filter(id -> id >= 1 && id <= 4).mapToInt(Integer::intValue).max().orElse(0);
        if (highest == 0) {
            stats.unlockedCovenants.remove("noden");
            stats.nodenCovenantLevel = 0;
        } else {
            if (!stats.unlockedCovenants.contains("noden")) stats.unlockedCovenants.add("noden");
            stats.nodenCovenantLevel = highest - 1;
        }
        if (stats.originalSpiritPartner > 0 && stats.originalSpirits.contains(stats.originalSpiritPartner)) {
            stats.activeCovenant = stats.originalSpiritPartner <= 4 ? "noden" : "spirit:" + stats.originalSpiritPartner;
        } else if ("noden".equals(stats.activeCovenant) || stats.activeCovenant.startsWith("spirit:")) stats.activeCovenant = "";
    }

    private static void initialize(BSPlayerStats stats) {
        if (stats.originalSpiritsInitialized) return;
        stats.originalSpiritsInitialized = true;
        if (stats.unlockedCovenants.contains("noden")) {
            int id = Math.max(0, Math.min(3, stats.nodenCovenantLevel)) + 1;
            if (!stats.originalSpirits.contains(id)) stats.originalSpirits.add(id);
            if ("noden".equals(stats.activeCovenant)) stats.originalSpiritPartner = id;
        }
    }

    public static void update(ServerPlayer player) {
        float hp = player.getHealth();
        double mp = stats(player).mp;
        StatEventHandler.applyStats(player);
        player.setHealth(Math.min(hp, player.getMaxHealth()));
        stats(player).mp = Math.max(0.0D, Math.min(mp, stats(player).maxMp));
        NetworkHandler.sendToPlayer(new PacketSyncStats(stats(player).serializeNBT()), player);
    }

    private static BSPlayerStats stats(ServerPlayer player) {
        if (player == null) throw new UnsupportedOperationException("original_spirit_player");
        return player.getCapability(BSPlayerStats.CAPABILITY).orElseThrow(() -> new IllegalStateException("Missing original spirit player capability"));
    }
}
