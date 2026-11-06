package yuki135.mmdskin.render.backend.mode;

import yuki135.mmdskin.asset.catalog.ModelCatalogEntry;
import yuki135.mmdskin.model.runtime.ModelInstance;

/**
 * MMD 模型工厂接口。
 */
public interface ModelInstanceFactory {

    RenderCategory getCategory();

    String getModeName();

    int getPriority();

    boolean isAvailable();

    default boolean isEnabledByDefault() {
        return false;
    }

    default boolean isEnabledInCurrentEnvironment() {
        return isEnabledByDefault();
    }

    default boolean supportsPMD() {
        return true;
    }

    ModelInstance createModel(String modelFilename, String modelDir, boolean isPMD, long layerCount);

    ModelInstance createModelFromHandle(long modelHandle, String modelDir);

    default ModelInstance createModel(ModelCatalogEntry modelInfo, long layerCount) {
        if (modelInfo == null) {
            return null;
        }
        return createModel(
            modelInfo.getModelFilePath(),
            modelInfo.getFolderPath(),
            modelInfo.isPMD(),
            layerCount
        );
    }
}
