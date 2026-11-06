package yuki135.mmdskin.ui.wheel.service;

import java.util.List;

public interface MorphWheelService {
    List<MorphOption> loadMorphs();

    void selectMorph(MorphOption option);
}
