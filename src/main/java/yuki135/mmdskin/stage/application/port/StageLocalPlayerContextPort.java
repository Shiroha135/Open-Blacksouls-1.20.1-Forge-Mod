package yuki135.mmdskin.stage.application.port;

import java.util.UUID;

public interface StageLocalPlayerContextPort {
    UUID getLocalPlayerUUID();

    String resolvePlayerName(UUID uuid);
}
