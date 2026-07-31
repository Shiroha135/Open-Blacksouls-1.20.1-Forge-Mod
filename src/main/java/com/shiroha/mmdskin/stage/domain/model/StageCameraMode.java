package com.shiroha.mmdskin.stage.domain.model;

public enum StageCameraMode {
    HOST_CAMERA,
    LOCAL_CAMERA;

    public static StageCameraMode fromUseHostCamera(boolean useHostCamera) {
        return useHostCamera ? HOST_CAMERA : LOCAL_CAMERA;
    }

    public boolean usesHostCamera() {
        return this == HOST_CAMERA;
    }
}
