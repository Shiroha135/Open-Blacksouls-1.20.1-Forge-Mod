package yuki135.mmdskin.stage.client.playback;

import yuki135.mmdskin.stage.domain.model.StageDescriptor;

public record StagePlaybackStartRequest(StageDescriptor descriptor, Float startFrame,
                                        Float hostHeightOffset, String motionPackName) {
    public StagePlaybackStartRequest {
        descriptor = descriptor != null ? descriptor.copy() : null;
        motionPackName = sanitizePackName(motionPackName);
    }

    private static String sanitizePackName(String packName) {
        return packName != null && !packName.isEmpty()
                && !packName.contains("..")
                && !packName.contains("/")
                && !packName.contains("\\")
                ? packName
                : null;
    }
}
