package yuki135.mmdskin.stage.application;

import yuki135.mmdskin.stage.domain.model.StageCameraMode;
import yuki135.mmdskin.stage.domain.model.StageMemberState;

import java.util.UUID;

public record StageSessionMemberSnapshot(UUID playerId, String playerName,
                                         StageMemberState state, StageCameraMode cameraMode) {
}
