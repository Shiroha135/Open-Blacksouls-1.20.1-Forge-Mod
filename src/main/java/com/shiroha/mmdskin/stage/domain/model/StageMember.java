package com.shiroha.mmdskin.stage.domain.model;

import java.util.UUID;

public record StageMember(UUID uuid,
                          String name,
                          StageMemberState state,
                          StageCameraMode cameraMode,
                          boolean local) {
}
