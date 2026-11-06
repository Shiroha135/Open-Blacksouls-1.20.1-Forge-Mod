package yuki135.mmdskin.ui.selector.adapter;

import yuki135.mmdskin.config.ModelConfigData;
import yuki135.mmdskin.config.ModelConfigManager;
import yuki135.mmdskin.ui.config.ModelSelectorConfig;
import yuki135.mmdskin.ui.selector.port.ModelSettingsGateway;

public class DefaultModelSettingsGateway implements ModelSettingsGateway {
    @Override
    public ModelConfigData loadConfig(String modelName) {
        return ModelConfigManager.getConfig(modelName).copy();
    }

    @Override
    public void saveConfig(String modelName, ModelConfigData config) {
        ModelConfigManager.saveConfig(modelName, config == null ? null : config.copy());
    }

    @Override
    public String getQuickSlotModel(int slot) {
        return ModelSelectorConfig.getInstance().getQuickSlotModel(slot);
    }

    @Override
    public void setQuickSlotModel(int slot, String modelName) {
        ModelSelectorConfig.getInstance().setQuickSlotModel(slot, modelName);
    }
}
