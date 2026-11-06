package yuki135.mmdskin.stage.client.bootstrap;

import yuki135.mmdskin.bridge.runtime.NativeAnimationBridgeHolder;
import yuki135.mmdskin.bridge.runtime.NativeRuntimeBridgeHolder;
import yuki135.mmdskin.stage.client.StageClientRuntime;

/** 文件职责：初始化 common 客户端舞台会话、播放与相机运行时。 */
public final class StageClientBootstrap {
    private StageClientBootstrap() {
    }

    public static void initialize() {
        StageClientRuntime.initialize(NativeRuntimeBridgeHolder.get(), NativeAnimationBridgeHolder.get());
    }
}
