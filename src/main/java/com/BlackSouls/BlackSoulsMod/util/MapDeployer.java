package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MapDeployer {

    private static final String[] CHUNK_FILES = {
            "r.0.0.mca", "r.0.-1.mca", "r.-1.0.mca", "r.-1.-1.mca"
    };

    public static void deploy(MinecraftServer server) {
        Path worldPath = server.getWorldPath(LevelResource.ROOT);
        Path dimPath = worldPath.resolve("dimensions").resolve(BlackSouls.MODID).resolve("library");

        Path regionDir = dimPath.resolve("region");
        Path entitiesDir = dimPath.resolve("entities");
        Path markerFile = dimPath.resolve("blacksouls_map_deployed.flag");

        try {
            if (Files.exists(markerFile)) {
                return;
            }

            Files.createDirectories(regionDir);
            Files.createDirectories(entitiesDir);

            boolean allSuccess = true;

            
            for (String fileName : CHUNK_FILES) {
                String internalPath = "/assets/" + BlackSouls.MODID + "/prebuilt_maps/" + fileName;
                try (InputStream in = MapDeployer.class.getResourceAsStream(internalPath)) {
                    if (in != null) {
                        Files.copy(in, regionDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        System.err.println("Missing map chunk file: " + internalPath);
                        allSuccess = false;
                    }
                }
            }

            
            for (String fileName : CHUNK_FILES) {
                String internalPath = "/assets/" + BlackSouls.MODID + "/prebuilt_entities/" + fileName;
                try (InputStream in = MapDeployer.class.getResourceAsStream(internalPath)) {
                    if (in != null) {
                        Files.copy(in, entitiesDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                    }
                }
            }

            if (allSuccess) {
                Files.createFile(markerFile); 
            }
        } catch (Exception e) {
            System.err.println("Failed to deploy library dimension data.");
            e.printStackTrace();
        }
    }
}
