package yuki135.mmdskin.render.backend.factory;

import yuki135.mmdskin.bridge.runtime.NativeRenderBackendPort;
import yuki135.mmdskin.model.runtime.ModelInstance;
import yuki135.mmdskin.render.backend.mode.ModelInstanceFactory;
import yuki135.mmdskin.render.backend.mode.RenderCategory;
import yuki135.mmdskin.render.backend.opengl.OpenGlModelInstance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * CPU 钂欑毊妯″瀷宸ュ巶銆?
 */
public class CpuSkinningBackendFactory implements ModelInstanceFactory {
    private static final Logger logger = LogManager.getLogger();

    private static final int PRIORITY = 0;
    private final NativeRenderBackendPort nativeRenderBackendPort;

    public CpuSkinningBackendFactory(NativeRenderBackendPort nativeRenderBackendPort) {
        if (nativeRenderBackendPort == null) {
            throw new IllegalArgumentException("nativeRenderBackendPort cannot be null");
        }
        this.nativeRenderBackendPort = nativeRenderBackendPort;
    }

    @Override
    public RenderCategory getCategory() {
        return RenderCategory.CPU_SKINNING;
    }

    @Override
    public String getModeName() {
        return "CPU钂欑毊";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {

        return true;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public ModelInstance createModel(String modelFilename, String modelDir, boolean isPMD, long layerCount) {
        try {
            return OpenGlModelInstance.create(nativeRenderBackendPort, modelFilename, modelDir, isPMD, layerCount);
        } catch (Throwable e) {
            logger.error("CPU ????????: {}", modelFilename, e);
            return null;
        }
    }

    @Override
    public ModelInstance createModelFromHandle(long modelHandle, String modelDir) {
        try {
            return OpenGlModelInstance.createFromHandle(nativeRenderBackendPort, modelHandle, modelDir);
        } catch (Throwable e) {
            logger.error("CPU ?????????????", e);
            return null;
        }
    }
}

