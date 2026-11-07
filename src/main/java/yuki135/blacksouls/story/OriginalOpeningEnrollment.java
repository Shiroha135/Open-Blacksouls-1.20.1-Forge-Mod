package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalOpeningEnrollment {
    private static final int MAX_PENDING = 2048;
    private static final long MAX_AGE = TimeUnit.MINUTES.toNanos(2);
    private static final Map<ServerPlayer, Attempt> PENDING = new IdentityHashMap<>();

    private OriginalOpeningEnrollment() {
    }

    public static void beginLoad(PlayerList list, PlayerDataStorage storage, ServerPlayer player) {
        synchronized (PENDING) {
            expire(System.nanoTime());
            Attempt previous = PENDING.get(player);
            if (previous != null) {
                previous.rejected = true;
                return;
            }
            MinecraftServer server = player.getServer();
            UUID uuid = player.getUUID();
            boolean duplicate = false;
            for (Attempt pending : PENDING.values()) {
                if (pending.server == server && pending.uuid.equals(uuid)) {
                    pending.rejected = true;
                    duplicate = true;
                }
            }
            if (duplicate || !trustedIdentity(list, storage, player, server, uuid)
                    || list.getPlayer(uuid) != null) return;
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (!uninitialized(stats) || hasCharacterEvidence(player, stats)) return;
            try {
                Path directory = trustedDirectory(server, storage);
                if (!noPriorFiles(server, directory, player)
                        || server.isSingleplayerOwner(player.getGameProfile())
                        && server.getWorldData().getLoadedPlayerTag() != null) return;
                if (PENDING.size() >= MAX_PENDING) return;
                PENDING.put(player, new Attempt(list, storage, server, uuid, stats, directory, System.nanoTime()));
            } catch (IOException | RuntimeException exception) {
                PENDING.remove(player);
            }
        }
    }

    public static void finishLoad(PlayerList list, PlayerDataStorage storage, ServerPlayer player, CompoundTag loaded) {
        synchronized (PENDING) {
            expire(System.nanoTime());
            Attempt attempt = PENDING.get(player);
            if (attempt == null) return;
            if (loaded != null || attempt.returned || attempt.hooks != 1
                    || !matches(attempt, list, storage, player) || !uninitialized(attempt.stats)
                    || hasCharacterEvidence(player, attempt.stats)) {
                PENDING.remove(player);
                return;
            }
            try {
                if (!noPriorFiles(attempt.server, attempt.directory, player)) {
                    PENDING.remove(player);
                    return;
                }
            } catch (IOException | RuntimeException exception) {
                PENDING.remove(player);
                return;
            }
            attempt.returned = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoadFromFile(PlayerEvent.LoadFromFile event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        synchronized (PENDING) {
            expire(System.nanoTime());
            Attempt attempt = PENDING.get(player);
            if (attempt == null) return;
            attempt.hooks++;
            try {
                Path directory = event.getPlayerDirectory().toPath().toAbsolutePath().normalize();
                if (attempt.hooks != 1 || attempt.returned || !matches(attempt, attempt.list, attempt.storage, player)
                        || !event.getPlayerUUID().equals(attempt.uuid.toString())
                        || !directory.equals(attempt.directory) || !directory.toRealPath().equals(directory)) {
                    attempt.rejected = true;
                }
            } catch (IOException | RuntimeException exception) {
                attempt.rejected = true;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        synchronized (PENDING) {
            expire(System.nanoTime());
            Attempt attempt = PENDING.remove(player);
            PENDING.entrySet().removeIf(entry -> entry.getValue().server == player.getServer()
                    && entry.getValue().uuid.equals(player.getUUID()));
            if (attempt == null || !attempt.returned || attempt.hooks != 1
                    || !matches(attempt, attempt.list, attempt.storage, player)
                    || attempt.list.getPlayer(attempt.uuid) != player || !uninitialized(attempt.stats)
                    || hasCharacterEvidence(player, attempt.stats)) return;
            attempt.stats.initializeOpeningProfile(OriginalOpeningProfile.createForVerifiedNewPlayer());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        synchronized (PENDING) {
            PENDING.entrySet().removeIf(entry -> entry.getKey() == player
                    || entry.getValue().server == player.getServer() && entry.getValue().uuid.equals(player.getUUID()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (PENDING) {
            PENDING.entrySet().removeIf(entry -> entry.getValue().server == event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        synchronized (PENDING) {
            expire(System.nanoTime());
        }
    }

    public static int pendingWitnessCount(MinecraftServer server) {
        synchronized (PENDING) {
            expire(System.nanoTime());
            return (int) PENDING.values().stream().filter(attempt -> attempt.server == server).count();
        }
    }

    public static boolean shouldDeferLegacyInitialRewards(ServerPlayer player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) return true;
        OriginalOpeningProfile profile = stats.openingProfile();
        return profile.previewActive() || profile.readState() == OriginalOpeningProfile.ReadState.REAL
                || profile.readState() == OriginalOpeningProfile.ReadState.REJECTED;
    }

    private static boolean trustedIdentity(PlayerList list, PlayerDataStorage storage, ServerPlayer player,
            MinecraftServer server, UUID uuid) {
        if (server == null || !server.isSameThread() || list != server.getPlayerList() || list.getServer() != server
                || storage == null || storage.getClass() != PlayerDataStorage.class
                || uuid == null || !uuid.equals(player.getGameProfile().getId())) return false;
        try {
            String method = ObfuscationReflectionHelper.findMethod(PlayerList.class, "m_11224_", ServerPlayer.class).getName();
            return list.getClass().getMethod(method, ServerPlayer.class).getDeclaringClass() == PlayerList.class;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean matches(Attempt attempt, PlayerList list, PlayerDataStorage storage, ServerPlayer player) {
        if (attempt.rejected || attempt.list != list || attempt.storage != storage) return false;
        try {
            if (!trustedDirectory(attempt.server, storage).equals(attempt.directory)) return false;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
        return !attempt.rejected && attempt.list == list && attempt.storage == storage
                && trustedIdentity(list, storage, player, attempt.server, attempt.uuid)
                && attempt.uuid.equals(player.getUUID())
                && player.getCapability(BSPlayerStats.CAPABILITY).orElse(null) == attempt.stats;
    }

    private static boolean uninitialized(BSPlayerStats stats) {
        if (stats == null) return false;
        OriginalOpeningProfile profile = stats.openingProfile();
        return profile.readState() == OriginalOpeningProfile.ReadState.ABSENT_LEGACY
                && !profile.hasStoredData() && !profile.hasRejectedData() && !profile.previewActive();
    }

    private static boolean hasCharacterEvidence(ServerPlayer player, BSPlayerStats stats) {
        if (stats.hasVisitedLibrary || stats.whiteBearIntroduced || stats.whiteBearFreeSoulsClaimed
                || stats.whiteBearProgress != 0 || stats.level != 1 || stats.currentExp != 0 || stats.souls != 0
                || stats.lostSouls != 0 || !stats.lostDim.isEmpty() || stats.developerNoCooldown || stats.developerLimitBreak
                || stats.bonusHp != 0 || stats.bonusMp != 0 || stats.bonusAtk != 0 || stats.bonusDef != 0
                || stats.bonusMatk != 0 || stats.bonusMdef != 0 || stats.bonusLuc != 0 || stats.bonusSpeed != 0
                || stats.sen != 100 || !stats.skillZ.isEmpty() || !stats.skillX.isEmpty()
                || !stats.skillC.isEmpty() || !stats.skillV.isEmpty()
                || stats.purgeRefreshCycle != -1 || stats.purgeRefreshDay != -1 || !stats.purgeTasks.isEmpty()
                || stats.purgeRefreshesUsedToday != 0 || stats.purgeMobKills != 0 || stats.purgeOreBreaks != 0
                || stats.purgeTrashEarned != 0 || !stats.activeCovenant.isEmpty() || stats.nodenCovenantLevel != 0
                || !stats.unlockedCovenants.isEmpty() || !stats.unlockedSkills.isEmpty() || !stats.unlockedBonfires.isEmpty()
                || stats.originalSpiritsInitialized || !stats.originalSpirits.isEmpty()
                || stats.originalSpiritPartner != 0 || !stats.originalCovenants.isEmpty()
                || stats.originalNodenProgressKnown || stats.originalNodenPledged
                || stats.originalNodenStage != 0 || stats.originalSenThresholdMask != 0
                || !player.getInventory().isEmpty() || !player.getEnderChestInventory().isEmpty()
                || player.experienceLevel != 0 || player.totalExperience != 0 || player.experienceProgress != 0) return true;
        return !player.getPersistentData().isEmpty();
    }

    private static Path trustedDirectory(MinecraftServer server, PlayerDataStorage storage) throws IOException {
        Path expected = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toAbsolutePath().normalize();
        Path actual = storage.getPlayerDataFolder().toPath().toAbsolutePath().normalize();
        if (!actual.equals(expected) || !actual.toRealPath().equals(actual)
                || !Files.isDirectory(actual, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(actual)) {
            throw new IOException("Unverified player storage");
        }
        return actual;
    }

    private static boolean noPriorFiles(MinecraftServer server, Path directory, ServerPlayer player) throws IOException {
        String uuid = player.getUUID().toString();
        Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if (!world.toRealPath().equals(world) || !noMatchingFiles(directory, uuid + "*")) return false;
        if (!noMatchingFiles(world.resolve("stats"), uuid + "*")
                || !noMatchingFiles(world.resolve("advancements"), uuid + "*")) return false;
        String name = player.getGameProfile().getName();
        if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) return false;
        Path legacy = world.resolve("players");
        return noMatchingFiles(legacy, name + ".dat*");
    }

    private static boolean noMatchingFiles(Path directory, String pattern) throws IOException {
        if (Files.notExists(directory, LinkOption.NOFOLLOW_LINKS)) return true;
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                || !directory.toRealPath().equals(directory) || !Files.isReadable(directory)) return false;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, pattern)) {
            return !files.iterator().hasNext();
        }
    }

    private static void expire(long now) {
        Iterator<Attempt> attempts = PENDING.values().iterator();
        while (attempts.hasNext()) {
            long age = now - attempts.next().started;
            if (age < 0 || age >= MAX_AGE) attempts.remove();
        }
    }

    private static final class Attempt {
        private final PlayerList list;
        private final PlayerDataStorage storage;
        private final MinecraftServer server;
        private final UUID uuid;
        private final BSPlayerStats stats;
        private final Path directory;
        private final long started;
        private int hooks;
        private boolean returned;
        private boolean rejected;

        private Attempt(PlayerList list, PlayerDataStorage storage, MinecraftServer server, UUID uuid,
                BSPlayerStats stats, Path directory, long started) {
            this.list = list;
            this.storage = storage;
            this.server = server;
            this.uuid = uuid;
            this.stats = stats;
            this.directory = directory;
            this.started = started;
        }
    }
}
