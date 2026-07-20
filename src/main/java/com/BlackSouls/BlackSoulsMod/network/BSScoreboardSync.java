package com.BlackSouls.BlackSoulsMod.network;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BSScoreboardSync {

    private static Boolean bukkitAvailable = null;
    private static Method bukkitGetPlayer;
    private static Method playerGetScoreboard;
    private static Method scoreboardGetObjective;
    private static Method scoreboardRegisterNewObjective;
    private static Method objectiveGetScore;
    private static Method scoreSetScore;
    private static final Map<UUID, PlayerScoreCache> PLAYER_SCORE_CACHE = new HashMap<>();
    private static long lastSyncFailureLogTime = 0L;

    private static final class PlayerScoreCache {
        private final Object scoreboard;
        private final Object soulsScore;
        private final Object manaScore;

        private PlayerScoreCache(Object scoreboard, Object soulsScore, Object manaScore) {
            this.scoreboard = scoreboard;
            this.soulsScore = soulsScore;
            this.manaScore = manaScore;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!event.side.isServer() || event.phase != TickEvent.Phase.END || event.player.tickCount % 10 != 0) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || !ensureBukkitReflection()) {
            return;
        }

        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            return;
        }

        UUID playerId = player.getUUID();
        try {
            Object bukkitPlayer = bukkitGetPlayer.invoke(null, playerId);
            if (bukkitPlayer == null) {
                PLAYER_SCORE_CACHE.remove(playerId);
                return;
            }

            Object currentScoreboard = playerGetScoreboard.invoke(bukkitPlayer);
            PlayerScoreCache cache = getOrCreateScoreCache(playerId, currentScoreboard, player.getScoreboardName());
            scoreSetScore.invoke(cache.soulsScore, (int) stats.souls);
            scoreSetScore.invoke(cache.manaScore, (int) stats.mp);
        } catch (ReflectiveOperationException | SecurityException e) {
            PLAYER_SCORE_CACHE.remove(playerId);
            logSyncFailure(e);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYER_SCORE_CACHE.remove(event.getEntity().getUUID());
    }

    private static boolean ensureBukkitReflection() {
        if (bukkitAvailable == Boolean.FALSE) {
            return false;
        }
        if (bukkitAvailable == Boolean.TRUE) {
            return true;
        }

        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Class<?> playerClass = Class.forName("org.bukkit.entity.Player");
            Class<?> scoreboardClass = Class.forName("org.bukkit.scoreboard.Scoreboard");
            Class<?> objectiveClass = Class.forName("org.bukkit.scoreboard.Objective");
            Class<?> scoreClass = Class.forName("org.bukkit.scoreboard.Score");

            bukkitGetPlayer = bukkitClass.getMethod("getPlayer", UUID.class);
            playerGetScoreboard = playerClass.getMethod("getScoreboard");
            scoreboardGetObjective = scoreboardClass.getMethod("getObjective", String.class);
            scoreboardRegisterNewObjective = scoreboardClass.getMethod("registerNewObjective", String.class, String.class, String.class);
            objectiveGetScore = objectiveClass.getMethod("getScore", String.class);
            scoreSetScore = scoreClass.getMethod("setScore", int.class);
            bukkitAvailable = Boolean.TRUE;
            return true;
        } catch (ClassNotFoundException e) {
            bukkitAvailable = Boolean.FALSE;
            BlackSouls.LOGGER.info("Bukkit not present (vanilla Forge environment). BlackSouls scoreboard->Bukkit sync disabled.");
            return false;
        } catch (ReflectiveOperationException | SecurityException e) {
            bukkitAvailable = Boolean.FALSE;
            BlackSouls.LOGGER.warn("Bukkit scoreboard API was found but could not be prepared. BlackSouls scoreboard->Bukkit sync disabled.", e);
            return false;
        }
    }

    private static Object getOrCreateObjective(Object scoreboard, String name, String displayName) throws ReflectiveOperationException {
        Object objective = scoreboardGetObjective.invoke(scoreboard, name);
        if (objective == null) {
            objective = scoreboardRegisterNewObjective.invoke(scoreboard, name, "dummy", displayName);
        }
        return objective;
    }

    private static PlayerScoreCache getOrCreateScoreCache(UUID playerId, Object scoreboard, String playerName) throws ReflectiveOperationException {
        PlayerScoreCache cache = PLAYER_SCORE_CACHE.get(playerId);
        if (cache != null && cache.scoreboard == scoreboard) {
            return cache;
        }

        Object soulsObjective = getOrCreateObjective(scoreboard, "bs_souls", "Souls");
        Object manaObjective = getOrCreateObjective(scoreboard, "bs_mana", "Mana");
        cache = new PlayerScoreCache(
                scoreboard,
                objectiveGetScore.invoke(soulsObjective, playerName),
                objectiveGetScore.invoke(manaObjective, playerName)
        );
        PLAYER_SCORE_CACHE.put(playerId, cache);
        return cache;
    }

    private static void logSyncFailure(Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastSyncFailureLogTime >= 15000L) {
            lastSyncFailureLogTime = now;
            BlackSouls.LOGGER.debug("Failed to sync BlackSouls scoreboard to Bukkit scoreboard.", e);
        }
    }
}
