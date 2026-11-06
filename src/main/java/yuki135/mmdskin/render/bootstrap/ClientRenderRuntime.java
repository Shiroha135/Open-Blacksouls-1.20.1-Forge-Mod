package yuki135.mmdskin.render.bootstrap;

import yuki135.mmdskin.bridge.runtime.NativeAnimationBridgeHolder;
import yuki135.mmdskin.bridge.runtime.NativeModelLoadPort;
import yuki135.mmdskin.bridge.runtime.NativeModelPort;
import yuki135.mmdskin.bridge.runtime.NativeModelQueryPort;
import yuki135.mmdskin.bridge.runtime.NativeMorphPort;
import yuki135.mmdskin.bridge.runtime.NativeRenderBackendPort;
import yuki135.mmdskin.bridge.runtime.NativeRuntimeBridgeHolder;
import yuki135.mmdskin.bridge.runtime.NativeRuntimePort;
import yuki135.mmdskin.bridge.runtime.NativeScenePort;
import yuki135.mmdskin.bridge.runtime.NativeTextureBridgeHolder;
import yuki135.mmdskin.bridge.runtime.PlatformCapabilityPort;
import yuki135.mmdskin.compat.vr.DefaultVrRuntimePort;
import yuki135.mmdskin.config.PhysicsConfigSnapshot;
import yuki135.mmdskin.config.RuntimeConfigPort;
import yuki135.mmdskin.config.RuntimeConfigPortHolder;
import yuki135.mmdskin.expression.ExpressionApplicationService;
import yuki135.mmdskin.expression.ModelMorphCatalog;
import yuki135.mmdskin.compat.maid.model.MaidModelRepositoryExtension;
import yuki135.mmdskin.model.port.ModelDiagnosticsPort;
import yuki135.mmdskin.model.port.ModelRepositoryPort;
import yuki135.mmdskin.model.runtime.DefaultModelRuntimeAccessPort;
import yuki135.mmdskin.model.runtime.ManagedModel;
import yuki135.mmdskin.model.runtime.ModelRepository;
import yuki135.mmdskin.player.render.ItemRenderHelper;
import yuki135.mmdskin.player.runtime.FirstPersonManager;
import yuki135.mmdskin.api.MmdSkinApi;
import yuki135.mmdskin.compat.vr.VRBoneDriver;
import yuki135.mmdskin.debug.client.PerformanceHud;
import yuki135.mmdskin.render.backend.RenderBackendRegistry;
import yuki135.mmdskin.render.pipeline.LivingEntityModelStateHelper;
import yuki135.mmdskin.render.port.RenderBackendSettingsPort;
import yuki135.mmdskin.scene.client.SceneModelManager;
import yuki135.mmdskin.texture.runtime.TextureRepository;

/** 文件职责：作为客户端渲染运行时的唯一组合根，统一持有仓储与后端服务。 */
public final class ClientRenderRuntime {
    private static volatile ClientRenderRuntime instance;

    private final NativeRuntimePort nativeRuntimePort;
    private final RuntimeConfigPort runtimeConfigPort;
    private final RenderBackendRegistry renderBackendRegistry;
    private final ModelRepository modelRepository;

    private ClientRenderRuntime() {
        this.nativeRuntimePort = NativeRuntimeBridgeHolder.get();
        this.runtimeConfigPort = RuntimeConfigPort.fromConfigManager();
        RuntimeConfigPortHolder.set(runtimeConfigPort);
        NativeRenderBackendPort nativeRenderBackendPort = nativeRuntimePort;
        PlatformCapabilityPort platformCapabilityPort = nativeRuntimePort;
        this.renderBackendRegistry = new RenderBackendRegistry(nativeRenderBackendPort, platformCapabilityPort, runtimeConfigPort);
        TextureRepository.Init();
        this.renderBackendRegistry.initialize();
        NativeModelPort modelPort = (NativeModelPort) nativeRuntimePort;
        NativeModelLoadPort modelLoadPort = (NativeModelLoadPort) nativeRuntimePort;
        NativeModelQueryPort modelQueryPort = (NativeModelQueryPort) nativeRuntimePort;
        NativeScenePort scenePort = (NativeScenePort) nativeRuntimePort;
        NativeMorphPort morphPort = (NativeMorphPort) nativeRuntimePort;
        this.modelRepository = new ModelRepository(
                new DefaultModelRuntimeAccessPort(renderBackendRegistry, modelLoadPort, modelPort),
                MaidModelRepositoryExtension.INSTANCE);
        ManagedModel.configureRuntimeCollaborators(NativeAnimationBridgeHolder.get(), nativeRuntimePort);
        ItemRenderHelper.configureRuntimeCollaborators(nativeRuntimePort);
        TextureRepository.configureRuntimeCollaborators(NativeTextureBridgeHolder.get());
        VRBoneDriver.configureRuntimeCollaborators(modelPort);
        PerformanceHud.configureRuntimeCollaborators(modelQueryPort);
        FirstPersonManager.configureRuntimeCollaborators(modelPort);
        FirstPersonManager.configureVrRuntime(new DefaultVrRuntimePort());
        LivingEntityModelStateHelper.configureRuntimeCollaborators(scenePort);
        SceneModelManager.getInstance().configureRuntimeCollaborators(scenePort);
        ExpressionApplicationService.configureRuntimeCollaborators(morphPort);
        ModelMorphCatalog.configureRuntimeCollaborators(modelQueryPort);
        MmdSkinApi.configureRuntimeCollaborators(modelPort, modelQueryPort);
    }

    public static synchronized void initialize() {
        if (instance == null) {
            instance = new ClientRenderRuntime();
        }
    }

    public static ClientRenderRuntime get() {
        if (instance == null) {
            throw new IllegalStateException("ClientRenderRuntime has not been initialized");
        }
        return instance;
    }

    public ModelRepositoryPort modelRepository() {
        return modelRepository;
    }

    public ModelDiagnosticsPort modelDiagnostics() {
        return modelRepository;
    }

    public RenderBackendSettingsPort renderBackendSettings() {
        return renderBackendRegistry;
    }

    public RenderBackendRegistry renderBackendRegistry() {
        return renderBackendRegistry;
    }

    public void applyPhysicsConfig(PhysicsConfigSnapshot physicsConfig) {
        nativeRuntimePort.applyPhysicsConfig(physicsConfig);
    }
}
