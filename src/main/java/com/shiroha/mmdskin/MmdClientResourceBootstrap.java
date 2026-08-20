package com.shiroha.mmdskin;

import com.shiroha.mmdskin.config.PathConstants;
import com.shiroha.mmdskin.config.UIConstants;
import com.shiroha.mmdskin.ui.config.ModelSelectorConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class MmdClientResourceBootstrap {
    public static final String BUNDLED_MODEL_NAME = "小红帽";

    private static final int BUFFER_SIZE = 8192;
    private static final long MAX_EXTRACTED_BYTES = 100L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 1024;
    private static final List<BundledModelSpec> BUNDLED_MODELS = List.of(
            new BundledModelSpec("小红帽", "/assets/mmdskin/bundled_models/red_hood.zip", ".blacksouls-red-hood-v1"),
            new BundledModelSpec("诺登", "/assets/mmdskin/bundled_models/noden.zip", ".blacksouls-noden-v1")
    );
    private static final String[] DEFAULT_ANIM_FILES = {
            "Drink.vmd", "crawl.vmd", "die.vmd", "elytraFly.vmd", "idle.vmd",
            "itemActive_minecraft.bow_Left_using.vmd", "itemActive_minecraft.iron_sword_Right_swinging.vmd",
            "itemActive_minecraft.shield_Left_using.vmd", "itemActive_minecraft.shield_Right_using.vmd",
            "lieDown.vmd", "onClimbable.vmd", "onClimbableDown.vmd", "onClimbableUp.vmd",
            "onHorse.vmd", "ride.vmd", "sleep.vmd", "sneak.vmd",
            "sprint.vmd", "swim.vmd", "swingLeft.vmd", "swingRight.vmd", "walk.vmd"
    };

    private MmdClientResourceBootstrap() {
    }

    static void initialize() {
        ensureDirectories();
        extractDefaultAnimIfNeeded();
        installBundledModelIfNeeded();
    }

    public static boolean selectBundledModelForPlayer(UUID playerId, String playerName) {
        if (playerId == null || playerName == null || playerName.isBlank() || !isBundledModelInstalled(BUNDLED_MODEL_NAME)) {
            return false;
        }

        Path marker = PathConstants.getConfigRootPath()
                .resolve("bundled_models")
                .resolve("red_hood")
                .resolve(playerId + ".selected");
        if (Files.exists(marker)) {
            return false;
        }

        boolean changed = false;
        try {
            ModelSelectorConfig selector = ModelSelectorConfig.getInstance();
            String selectedModel = selector.getPlayerModel(playerName);
            if (selectedModel == null || selectedModel.isBlank() || UIConstants.DEFAULT_MODEL_NAME.equals(selectedModel)) {
                selector.setPlayerModel(playerName, BUNDLED_MODEL_NAME);
                selector.saveNow();
                changed = true;
            }

            Files.createDirectories(marker.getParent());
            try {
                Files.writeString(marker, BUNDLED_MODEL_NAME, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            } catch (FileAlreadyExistsException ignored) {
            }
        } catch (IOException e) {
            MmdSkinClient.logger.warn("记录内置模型首次选择状态失败", e);
        }
        return changed;
    }

    private static void ensureDirectories() {
        PathConstants.ensureDirectoryExists(PathConstants.getSkinRootDir());
        PathConstants.ensureDirectoryExists(PathConstants.getEntityPlayerDir());
        PathConstants.ensureDirectoryExists(PathConstants.getCustomAnimDir());
        PathConstants.ensureDirectoryExists(PathConstants.getCustomMorphDir());
        PathConstants.ensureDirectoryExists(PathConstants.getDefaultMorphDir());
        PathConstants.ensureDirectoryExists(PathConstants.getSceneModelDir());
    }

    private static void extractDefaultAnimIfNeeded() {
        File defaultAnimDir = PathConstants.getDefaultAnimDir();
        PathConstants.ensureDirectoryExists(defaultAnimDir);

        for (String fileName : DEFAULT_ANIM_FILES) {
            File targetFile = new File(defaultAnimDir, fileName);
            if (targetFile.exists()) {
                continue;
            }

            try (InputStream input = MmdSkinClient.class.getResourceAsStream("/assets/mmdskin/default_anim/" + fileName)) {
                if (input != null) {
                    Files.copy(input, targetFile.toPath());
                    MmdSkinClient.logger.info("提取默认动画: {}", fileName);
                }
            } catch (IOException e) {
                MmdSkinClient.logger.warn("提取动画文件失败: " + fileName, e);
            }
        }
    }

    private static void installBundledModelIfNeeded() {
        for (BundledModelSpec spec : BUNDLED_MODELS) {
            installBundledModel(spec);
        }
    }

    private static void installBundledModel(BundledModelSpec spec) {
        Path targetDirectory = PathConstants.getModelDir(spec.name()).toPath().toAbsolutePath().normalize();
        Path marker = targetDirectory.resolve(spec.marker());
        if (Files.isRegularFile(marker) && isBundledModelInstalled(spec.name())) {
            return;
        }

        try (InputStream resource = MmdClientResourceBootstrap.class.getResourceAsStream(spec.resourcePath())) {
            if (resource == null) {
                MmdSkinClient.logger.error("内置模型资源缺失: {}", spec.resourcePath());
                return;
            }

            Path parent = targetDirectory.getParent();
            Files.createDirectories(parent);
            Path staging = Files.createTempDirectory(parent, ".mmdskin-bundle-");
            try {
                extractZip(resource, staging);
                if (!Files.isRegularFile(staging.resolve("model.pmx"))) {
                    throw new IOException("内置模型缺少 model.pmx: " + spec.name());
                }
                deleteRecursively(targetDirectory);
                Files.move(staging, targetDirectory);
                Files.writeString(marker, "v1", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                MmdSkinClient.logger.info("内置 MMD 模型已准备: {}", spec.name());
            } finally {
                deleteRecursively(staging);
            }
        } catch (IOException | RuntimeException e) {
            MmdSkinClient.logger.error("释放内置 MMD 模型失败", e);
        }
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    MmdSkinClient.logger.warn("删除内置模型残留文件失败: {}", p, e);
                }
            });
        } catch (IOException e) {
            MmdSkinClient.logger.warn("遍历内置模型目录失败: {}", path, e);
        }
    }

    private static boolean isBundledModelInstalled(String modelName) {
        return Files.isRegularFile(PathConstants.getModelDir(modelName).toPath().resolve("model.pmx"));
    }

    private static void extractZip(InputStream resource, Path targetDirectory) throws IOException {
        int entries = 0;
        long totalBytes = 0L;
        byte[] buffer = new byte[BUFFER_SIZE];

        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(resource), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw new IOException("内置模型文件数量超出限制");
                }

                Path output = targetDirectory.resolve(entry.getName()).normalize();
                if (!output.startsWith(targetDirectory)) {
                    throw new IOException("内置模型包含越界路径: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    zip.closeEntry();
                    continue;
                }

                Files.createDirectories(output.getParent());
                Path temporary = Files.createTempFile(output.getParent(), ".mmdskin-", ".tmp");
                OutputStream destination = new BufferedOutputStream(Files.newOutputStream(temporary));

                try (OutputStream out = destination) {
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        totalBytes += read;
                        if (totalBytes > MAX_EXTRACTED_BYTES) {
                            throw new IOException("内置模型解压体积超出限制");
                        }
                        out.write(buffer, 0, read);
                    }
                } catch (IOException e) {
                    Files.deleteIfExists(temporary);
                    throw e;
                }

                try {
                    Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException atomicMoveError) {
                    Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
    }

    private record BundledModelSpec(String name, String resourcePath, String marker) {
    }
}
