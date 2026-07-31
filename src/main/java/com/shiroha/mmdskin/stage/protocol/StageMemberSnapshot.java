package com.shiroha.mmdskin.stage.protocol;

public final class StageMemberSnapshot {
    public String uuid;
    public String name;
    public String state;
    public String cameraMode;

    public StageMemberSnapshot() {
    }

    public StageMemberSnapshot(String uuid, String name, String state, String cameraMode) {
        this.uuid = uuid;
        this.name = name;
        this.state = state;
        this.cameraMode = cameraMode;
    }
}
