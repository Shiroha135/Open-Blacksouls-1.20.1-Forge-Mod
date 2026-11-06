package yuki135.mmdskin.stage.server.application.port;

import yuki135.mmdskin.stage.protocol.StagePacket;
import yuki135.mmdskin.stage.server.domain.model.StageServerPlayer;

import java.util.List;
import java.util.UUID;

public interface StageServerPlatformPort {
    StageServerPlayer findPlayer(UUID playerId);

    List<StageServerPlayer> getOnlinePlayers();

    void sendPacket(UUID targetPlayerId, UUID sourcePlayerId, StagePacket packet);
}
