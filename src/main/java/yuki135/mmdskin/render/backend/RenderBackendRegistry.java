package yuki135.mmdskin.render.backend;

import yuki135.mmdskin.bridge.runtime.NativeRenderBackendPort;
import yuki135.mmdskin.bridge.runtime.PlatformCapabilityPort;
import yuki135.mmdskin.config.RuntimeConfigPort;
import yuki135.mmdskin.model.runtime.ModelInstance;
import yuki135.mmdskin.render.backend.factory.ModelFactoryRegistry;
import yuki135.mmdskin.render.backend.mode.RenderModeManager;
import yuki135.mmdskin.render.port.RenderBackendSettingsPort;

/** 文件职责：封装渲染后端注册、选择与配置入口。 */
public final class RenderBackendRegistry implements RenderBackendSettingsPort {
    private final NativeRenderBackendPort nativeRenderBackendPort;
    private final PlatformCapabilityPort platformCapabilityPort;
    private final RuntimeConfigPort runtimeConfigPort;
    private volatile boolean shaderEnabled;
    private volatile int shaderPipelineMode;

    public RenderBackendRegistry(NativeRenderBackendPort nativeRenderBackendPort,
                                 PlatformCapabilityPort platformCapabilityPort,
                                 RuntimeConfigPort runtimeConfigPort) {
        if (nativeRenderBackendPort == null) {
            throw new IllegalArgumentException("nativeRenderBackendPort cannot be null");
        }
        if (platformCapabilityPort == null) {
            throw new IllegalArgumentException("platformCapabilityPort cannot be null");
        }
        if (runtimeConfigPort == null) {
            throw new IllegalArgumentException("runtimeConfigPort cannot be null");
        }
        this.nativeRenderBackendPort = nativeRenderBackendPort;
        this.platformCapabilityPort = platformCapabilityPort;
        this.runtimeConfigPort = runtimeConfigPort;
    }

    public void initialize() {
        ModelFactoryRegistry.registerAll(nativeRenderBackendPort, platformCapabilityPort);
        RenderModeManager.init();
        applyConfig();
    }

    public ModelInstance createModelFromHandle(long modelHandle, String modelDir) {
        return RenderModeManager.createModelFromHandle(modelHandle, modelDir);
    }

    public ModelInstance createModel(String modelFilename, String modelDir, long layerCount) {
        return RenderModeManager.createModel(modelFilename, modelDir, layerCount);
    }

    @Override
    public void setGpuSkinningEnabled(boolean enabled) {
        RenderModeManager.setUseGpuSkinning(enabled);
    }

    @Override
    public boolean isGpuSkinningEnabled() {
        return RenderModeManager.isUseGpuSkinning();
    }

    @Override
    public void setShaderEnabled(boolean enabled) {
        this.shaderEnabled = enabled;
    }

    @Override
    public boolean isShaderEnabled() {
        return shaderEnabled;
    }

    public int shaderPipelineMode() {
        return shaderPipelineMode;
    }

    public void setShaderPipelineMode(int shaderPipelineMode) {
        this.shaderPipelineMode = shaderPipelineMode;
    }

    @Override
    public String currentModeDescription() {
        return RenderModeManager.getCurrentModeDescription();
    }

    private void applyConfig() {
        setGpuSkinningEnabled(runtimeConfigPort.isGpuSkinningEnabled());
        setShaderEnabled(runtimeConfigPort.isMmdShaderEnabled());
    }
}
