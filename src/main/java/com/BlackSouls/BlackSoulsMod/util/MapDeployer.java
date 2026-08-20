package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class MapDeployer {
    public static final String MAP_VERSION = "hakoniwa-v4";
    private static final String MAP_ARCHIVE = "/assets/" + BlackSouls.MODID + "/prebuilt_maps/hakoniwa-v2.zip";
    private static final String LOCK_DATA_RESOURCE = "/assets/" + BlackSouls.MODID + "/prebuilt_maps/blacksouls_door_locks.dat";
    private static final String LOCK_DATA_NAME = "blacksouls_door_locks.dat";
    private static final String ANIMATED_DOOR_DATA_RESOURCE = "/assets/" + BlackSouls.MODID + "/prebuilt_maps/blacksouls_animated_doors.dat";
    private static final String ANIMATED_DOOR_DATA_NAME = "blacksouls_animated_doors.dat";
    private static final String DOOR_EVENT_DATA_RESOURCE = "/assets/" + BlackSouls.MODID + "/prebuilt_maps/blacksouls_door_events.dat";
    private static final String DOOR_EVENT_DATA_NAME = "blacksouls_door_events.dat";
    private static final String MARKER_NAME = "blacksouls_" + MAP_VERSION + ".flag";
    private static final Map<String, Long> EXPECTED_FILES = Map.of(
            "region", 64L,
            "entities", 29L,
            "poi", 29L
    );

    public static void deploy(MinecraftServer server) {
        Path worldPath = server.getWorldPath(LevelResource.ROOT);
        Path dimensionRoot = worldPath.resolve("dimensions").resolve(BlackSouls.MODID);
        Path dimensionPath = dimensionRoot.resolve("hokoniwa");
        migrateLegacyDimension(dimensionRoot.resolve("library"), dimensionPath);
        Path marker = dimensionPath.resolve(MARKER_NAME);
        deploySavedDataIfMissing(dimensionPath, LOCK_DATA_RESOURCE, LOCK_DATA_NAME);
        deploySavedDataIfMissing(dimensionPath, ANIMATED_DOOR_DATA_RESOURCE, ANIMATED_DOOR_DATA_NAME);
        deploySavedDataIfMissing(dimensionPath, DOOR_EVENT_DATA_RESOURCE, DOOR_EVENT_DATA_NAME);
        if (Files.exists(marker) && isMapPresent(dimensionPath)) {
            return;
        }

        Path staging = dimensionPath.resolve("." + MAP_VERSION + "-" + UUID.randomUUID());
        try {
            Files.createDirectories(staging);
            extractArchive(staging);
            validateArchive(staging);
            Files.createDirectories(dimensionPath);
            for (String folder : EXPECTED_FILES.keySet()) {
                Path target = dimensionPath.resolve(folder);
                deleteRecursively(target);
                Files.move(staging.resolve(folder), target, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(
                    marker,
                    MAP_VERSION,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            BlackSouls.LOGGER.info("Deployed {} map data", MAP_VERSION);
        } catch (Exception exception) {
            BlackSouls.LOGGER.error("Failed to deploy {} map data", MAP_VERSION, exception);
        } finally {
            try {
                deleteRecursively(staging);
            } catch (IOException exception) {
                BlackSouls.LOGGER.warn("Failed to clean map staging directory {}", staging, exception);
            }
        }
    }

    private static void migrateLegacyDimension(Path legacyPath, Path targetPath) {
        if (!Files.isDirectory(legacyPath) || Files.exists(targetPath)) {
            return;
        }
        try {
            Files.createDirectories(targetPath.getParent());
            try {
                Files.move(legacyPath, targetPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveError) {
                Files.move(legacyPath, targetPath);
            }
            BlackSouls.LOGGER.info("Migrated legacy dimension data from {} to {}", legacyPath, targetPath);
        } catch (IOException exception) {
            BlackSouls.LOGGER.error("Failed to migrate legacy dimension data from {} to {}", legacyPath, targetPath, exception);
        }
    }

    private static void extractArchive(Path staging) throws IOException {
        Path stagingRoot = staging.toAbsolutePath().normalize();
        try (InputStream resource = MapDeployer.class.getResourceAsStream(MAP_ARCHIVE)) {
            if (resource == null) {
                throw new IOException("Missing map archive: " + MAP_ARCHIVE);
            }
            try (ZipInputStream zip = new ZipInputStream(resource)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    Path relative = Path.of(entry.getName().replace('\\', '/')).normalize();
                    if (relative.getNameCount() != 2
                            || !EXPECTED_FILES.containsKey(relative.getName(0).toString())
                            || !relative.getFileName().toString().endsWith(".mca")) {
                        throw new IOException("Invalid map archive entry: " + entry.getName());
                    }
                    Path target = stagingRoot.resolve(relative).normalize();
                    if (!target.startsWith(stagingRoot)) {
                        throw new IOException("Map archive entry escapes staging: " + entry.getName());
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void validateArchive(Path staging) throws IOException {
        Map<String, Long> counts;
        try (Stream<Path> files = Files.walk(staging)) {
            counts = files
                    .filter(Files::isRegularFile)
                    .collect(Collectors.groupingBy(
                            path -> staging.relativize(path).getName(0).toString(),
                            Collectors.counting()
                    ));
        }
        boolean incomplete = EXPECTED_FILES.entrySet().stream()
                .anyMatch(expected -> counts.getOrDefault(expected.getKey(), 0L) < expected.getValue());
        if (incomplete || !Files.isRegularFile(staging.resolve("region").resolve("r.-1.0.mca"))) {
            throw new IOException("Incomplete map archive: " + counts);
        }
    }

    private static boolean isMapPresent(Path dimensionPath) {
        try {
            for (Map.Entry<String, Long> expected : EXPECTED_FILES.entrySet()) {
                Path folder = dimensionPath.resolve(expected.getKey());
                if (!Files.isDirectory(folder)) {
                    return false;
                }
                try (Stream<Path> files = Files.list(folder)) {
                    long count = files
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".mca"))
                            .count();
                    if (count < expected.getValue()) {
                        return false;
                    }
                }
            }
            Path entryRegion = dimensionPath.resolve("region").resolve("r.-1.0.mca");
            return Files.isRegularFile(entryRegion) && Files.size(entryRegion) > 0L;
        } catch (IOException exception) {
            return false;
        }
    }

    private static void deploySavedDataIfMissing(Path dimensionPath, String resourcePath, String dataName) {
        Path target = dimensionPath.resolve("data").resolve(dataName);
        if (Files.isRegularFile(target)) {
            return;
        }
        Path temporary = target.resolveSibling("." + dataName + "-" + UUID.randomUUID() + ".tmp");
        try (InputStream resource = MapDeployer.class.getResourceAsStream(resourcePath)) {
            if (resource == null) {
                throw new IOException("Missing map saved data: " + resourcePath);
            }
            Files.createDirectories(target.getParent());
            Files.copy(resource, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            BlackSouls.LOGGER.info("Deployed {} map saved data {}", MAP_VERSION, dataName);
        } catch (Exception exception) {
            BlackSouls.LOGGER.error("Failed to deploy {} map saved data {}", MAP_VERSION, dataName, exception);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException exception) {
                BlackSouls.LOGGER.warn("Failed to clean map lock staging file {}", temporary, exception);
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path target : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(target);
            }
        }
    }

    private MapDeployer() {
    }
}
