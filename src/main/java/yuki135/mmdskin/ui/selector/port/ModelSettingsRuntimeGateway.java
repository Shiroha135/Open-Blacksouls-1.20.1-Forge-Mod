package yuki135.mmdskin.ui.selector.port;

import yuki135.mmdskin.config.ModelConfigData;

public interface ModelSettingsRuntimeGateway {
    void applyConfigIfSelected(String modelName, ModelConfigData config);
}
