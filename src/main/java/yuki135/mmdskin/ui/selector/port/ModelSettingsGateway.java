package yuki135.mmdskin.ui.selector.port;

import yuki135.mmdskin.config.ModelConfigData;

public interface ModelSettingsGateway {
    ModelConfigData loadConfig(String modelName);

    void saveConfig(String modelName, ModelConfigData config);

    String getQuickSlotModel(int slot);

    void setQuickSlotModel(int slot, String modelName);
}
