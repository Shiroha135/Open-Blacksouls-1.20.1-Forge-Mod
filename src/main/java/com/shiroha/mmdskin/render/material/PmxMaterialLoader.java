package com.shiroha.mmdskin.render.material;

import com.shiroha.mmdskin.texture.runtime.TextureRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class PmxMaterialLoader {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void enrich(ModelMaterial[] materials,
                              String modelFilename,
                              String modelDir,
                              List<String> textureKeys) {
        Path modelPath = findModelPath(modelFilename, modelDir);
        if (modelPath == null) {
            return;
        }

        try {
            List<PmxMaterialParser.Material> parsed = PmxMaterialParser.read(modelPath);
            int count = Math.min(materials.length, parsed.size());
            for (int i = 0; i < count; i++) {
                ModelMaterial material = materials[i];
                PmxMaterialParser.Material definition = parsed.get(i);
                material.apply(definition);
                if (material.tex == 0 && !definition.texturePath().isEmpty()) {
                    LoadedTexture texture = loadTexture(modelDir, definition.texturePath(), textureKeys);
                    if (texture != null) {
                        material.tex = texture.id();
                        material.hasAlpha = texture.hasAlpha();
                        material.texturePath = texture.path();
                    }
                }
                LoadedTexture toon = loadTexture(modelDir, definition.toonTexturePath(), textureKeys);
                if (toon != null) {
                    material.toonTex = toon.id();
                    material.toonTexturePath = toon.path();
                }
                LoadedTexture sphere = loadTexture(modelDir, definition.sphereTexturePath(), textureKeys);
                if (sphere != null) {
                    material.sphereTex = sphere.id();
                    material.sphereTexturePath = sphere.path();
                }
                material.updateStudioDefaults(modelPath.getFileName().toString());
            }
            LOGGER.info("Loaded PMX render materials for {}: {}/{}", modelPath.getFileName(), count, materials.length);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to read PMX render materials from {}", modelPath, exception);
        }
    }

    private static Path findModelPath(String modelFilename, String modelDir) {
        if (modelFilename != null && !modelFilename.isBlank()) {
            Path supplied = Path.of(modelFilename);
            if (Files.isRegularFile(supplied) && supplied.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pmx")) {
                return supplied;
            }
        }

        if (modelDir == null || modelDir.isBlank()) {
            return null;
        }
        Path directory = Path.of(modelDir);
        Path conventional = directory.resolve("model.pmx");
        if (Files.isRegularFile(conventional)) {
            return conventional;
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pmx"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException exception) {
            return null;
        }
    }

    private static LoadedTexture loadTexture(String modelDir, String relativePath, List<String> textureKeys) {
        if (modelDir == null || relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String normalizedRelative = relativePath.replace('\\', '/');
        Path rawPath = Path.of(normalizedRelative);
        Path path = rawPath.isAbsolute() ? rawPath.normalize() : Path.of(modelDir).resolve(rawPath).normalize();
        if (!Files.isRegularFile(path)) {
            return null;
        }
        String key = path.toAbsolutePath().toString();
        TextureRepository.Texture texture = TextureRepository.GetTexture(key);
        if (texture == null) {
            return null;
        }
        TextureRepository.addRef(key);
        textureKeys.add(key);
        return new LoadedTexture(texture.tex, texture.hasAlpha, key);
    }

    private record LoadedTexture(int id, boolean hasAlpha, String path) {
    }

    private PmxMaterialLoader() {
    }
}
