package com.shiroha.mmdskin.stage.protocol;

import com.shiroha.mmdskin.stage.domain.model.StageCameraMode;
import com.shiroha.mmdskin.stage.domain.model.StageDescriptor;
import com.shiroha.mmdskin.stage.domain.model.StageInviteDecision;

import java.util.Collections;
import java.util.List;

public final class StagePacket {
    public int version = 3;
    public StagePacketType type;
    public String sessionId;
    public String targetPlayerId;
    public StageInviteDecision inviteDecision;
    public Boolean ready;
    public StageCameraMode cameraMode;
    public Float frame;
    public Float heightOffset;
    public StageDescriptor descriptor;
    public String motionPackName;
    public List<String> motionFiles = Collections.emptyList();
    public List<StageMemberSnapshot> members = Collections.emptyList();

    public StagePacket() {
    }

    public StagePacket(StagePacketType type) {
        this.type = type;
    }
}
