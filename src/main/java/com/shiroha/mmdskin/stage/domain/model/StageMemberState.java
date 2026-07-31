package com.shiroha.mmdskin.stage.domain.model;

public enum StageMemberState {
    HOST,
    INVITED,
    ACCEPTED,
    READY,
    DECLINED,
    BUSY;

    public boolean isActiveParticipant() {
        return this == HOST || this == ACCEPTED || this == READY;
    }

    public boolean isAcceptedState() {
        return this == ACCEPTED || this == READY;
    }
}
