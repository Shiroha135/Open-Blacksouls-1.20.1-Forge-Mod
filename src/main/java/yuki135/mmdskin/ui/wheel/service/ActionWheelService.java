package yuki135.mmdskin.ui.wheel.service;

import java.util.List;

public interface ActionWheelService {
    List<ActionOption> loadActions();

    void selectAction(String animId);
}
