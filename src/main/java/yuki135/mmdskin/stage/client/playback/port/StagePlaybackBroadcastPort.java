package yuki135.mmdskin.stage.client.playback.port;

import yuki135.mmdskin.stage.domain.model.StageDescriptor;

/** 定义舞台播放广播边界。 */
public interface StagePlaybackBroadcastPort {
    void sendStageWatch(StagePlaybackWatchRequest request);

    void sendRemoteStageStart(StageDescriptor descriptor);
}
