package yuki135.mmdskin.asset.catalog;

import yuki135.mmdskin.config.PathConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/** 文件职责：扫描并缓存本地模型目录的元数据条目。 */
public class ModelCatalogEntry {
    private static final Logger logger = LogManager.getLogger();

    private record CacheSnapshot(List<ModelCatalogEntry> models, long timestamp) {}
    private static volatile CacheSnapshot cache = null;
    private static final long CACHE_TTL = 5000;

    private final String folderName;
    private final String folderPath;
    private final String modelFilePath;
    private final String modelFileName;
    private final long fileSize;

    public ModelCatalogEntry(String folderName, String folderPath, String modelFilePath, String modelFileName, long fileSize) {
        this.folderName = folderName;
        this.folderPath = folderPath;
        this.modelFilePath = modelFilePath;
        this.modelFileName = modelFileName;
        this.fileSize = fileSize;
    }

    public String getFolderName() { return folderName; }
    public String getDisplayName() { return folderName; }
    public String getFolderPath() { return folderPath; }
    public String getModelFilePath() { return modelFilePath; }
    public String getModelFileName() { return modelFileName; }
    public long getFileSize() { return fileSize; }
    public String getCatalogKey() { return folderName; }

    public String getFormattedSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    public static List<ModelCatalogEntry> scanModels() {
        CacheSnapshot snapshot = cache;
        long now = System.currentTimeMillis();
        if (snapshot != null && (now - snapshot.timestamp()) < CACHE_TTL) {
            return snapshot.models();
        }

        List<ModelCatalogEntry> models = new ArrayList<>();
        File entityPlayerDir = PathConstants.getEntityPlayerDir();

        if (!entityPlayerDir.exists() || !entityPlayerDir.isDirectory()) {
            logger.warn("EntityPlayer 目录不存在: {}", entityPlayerDir.getAbsolutePath());
            return models;
        }

        File[] modelDirs = entityPlayerDir.listFiles(File::isDirectory);
        if (modelDirs == null) {
            return models;
        }

        for (File modelDir : modelDirs) {
            ModelCatalogEntry info = scanModelFolder(modelDir);
            if (info != null) {
                models.add(info);
                logger.debug("发现模型: {} -> {}", info.getFolderName(), info.getModelFileName());
            }
        }

        models.sort((a, b) -> a.getFolderName().compareToIgnoreCase(b.getFolderName()));
        cache = new CacheSnapshot(models, System.currentTimeMillis());
        return models;
    }

    public static void invalidateCache() {
        cache = null;
    }

    public String getFormatDescription() { return "PMX"; }

    private static ModelCatalogEntry scanModelFolder(File directory) {
        File[] models = directory.listFiles(file -> file.isFile() && file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pmx"));
        if (models == null || models.length == 0) return null;
        File model = findPreferredModel(models);
        return new ModelCatalogEntry(directory.getName(), directory.getAbsolutePath(), model.getAbsolutePath(), model.getName(), model.length());
    }

    private static File findPreferredModel(File[] files) {
        if (files.length == 1) {
            return files[0];
        }
        for (File file : files) {
            String name = file.getName().toLowerCase();
            if (name.equals("model.pmx")) {
                return file;
            }
        }
        java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return files[0];
    }

    public static ModelCatalogEntry findByFolderName(String folderName) {
        List<ModelCatalogEntry> models = scanModels();
        for (ModelCatalogEntry info : models) {
            if (info.getFolderName().equals(folderName)) {
                return info;
            }
        }
        return null;
    }
}
