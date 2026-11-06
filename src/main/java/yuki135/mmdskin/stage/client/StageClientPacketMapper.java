package yuki135.mmdskin.stage.client;

import yuki135.mmdskin.stage.application.StageSessionMemberSnapshot;
import yuki135.mmdskin.stage.client.playback.StagePlaybackStartRequest;
import yuki135.mmdskin.stage.domain.model.StageCameraMode;
import yuki135.mmdskin.stage.domain.model.StageDescriptor;
import yuki135.mmdskin.stage.domain.model.StageMemberState;
import yuki135.mmdskin.stage.protocol.StageMemberSnapshot;
import yuki135.mmdskin.stage.protocol.StagePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class StageClientPacketMapper {
    private StageClientPacketMapper() {
    }

    static UUID parseUUID(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static List<StageSessionMemberSnapshot> toSessionMembers(List<StageMemberSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }
        List<StageSessionMemberSnapshot> result = new ArrayList<>();
        for (StageMemberSnapshot snapshot : snapshots) {
            if (snapshot == null) {
                continue;
            }
            UUID playerId = parseUUID(snapshot.uuid);
            StageMemberState state = parseState(snapshot.state);
            if (playerId == null || state == null) {
                continue;
            }
            result.add(new StageSessionMemberSnapshot(
                    playerId,
                    snapshot.name,
                    state,
                    parseCameraMode(snapshot.cameraMode)
            ));
        }
        return List.copyOf(result);
    }

    static StagePlaybackStartRequest toPlaybackStartRequest(StagePacket packet) {
        if (packet == null) {
            return null;
        }
        StageDescriptor descriptor = packet.descriptor != null ? packet.descriptor.copy() : null;
        return new StagePlaybackStartRequest(descriptor, packet.frame, packet.heightOffset, packet.motionPackName);
    }

    private static StageMemberState parseState(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return StageMemberState.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static StageCameraMode parseCameraMode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return StageCameraMode.HOST_CAMERA;
        }
        try {
            return StageCameraMode.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return StageCameraMode.HOST_CAMERA;
        }
    }
}
