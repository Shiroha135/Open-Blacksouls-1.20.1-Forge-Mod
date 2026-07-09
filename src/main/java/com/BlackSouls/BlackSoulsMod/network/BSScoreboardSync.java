package com.BlackSouls.BlackSoulsMod.network;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
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
    private static long lastSyncFailureLogTime = 0L;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!event.side.isServer() || event.phase != TickEvent.Phase.END || event.player.tickCount % 10 != 0) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || !ensureBukkitReflection()) {
            return;
        }

        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            try {
                Object bukkitPlayer = bukkitGetPlayer.invoke(null, player.getUUID());
                if (bukkitPlayer == null) return;

                Object currentScoreboard = playerGetScoreboard.invoke(bukkitPlayer);
                String playerName = player.getScoreboardName();

                Object soulsObj = getOrCreateObjective(currentScoreboard, "bs_souls", "Souls");
                setScore(soulsObj, playerName, (int) stats.souls);

                Object manaObj = getOrCreateObjective(currentScoreboard, "bs_mana", "Mana");
                setScore(manaObj, playerName, (int) stats.mp);
            } catch (ReflectiveOperationException | SecurityException e) {
                logSyncFailure(e);
            }
        });
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

    private static void setScore(Object objective, String playerName, int value) throws ReflectiveOperationException {
        Object score = objectiveGetScore.invoke(objective, playerName);
        scoreSetScore.invoke(score, value);
    }

    private static void logSyncFailure(Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastSyncFailureLogTime >= 15000L) {
            lastSyncFailureLogTime = now;
            BlackSouls.LOGGER.debug("Failed to sync BlackSouls scoreboard to Bukkit scoreboard.", e);
        }
    }
}
