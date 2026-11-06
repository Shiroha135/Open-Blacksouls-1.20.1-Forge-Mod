package yuki135.mmdskin.ui.selector.adapter;

import yuki135.mmdskin.model.runtime.ModelRequestKey;
import yuki135.mmdskin.player.sync.PlayerModelSelectionSyncService;
import yuki135.mmdskin.render.bootstrap.ClientRenderRuntime;
import yuki135.mmdskin.ui.selector.port.ModelSelectionRuntimeGateway;
import net.minecraft.client.Minecraft;

/** 文件职责：在本地玩家切换模型后刷新对应仓储条目。 */
public class DefaultModelSelectionRuntimeGateway implements ModelSelectionRuntimeGateway {
    @Override
    public void afterLocalModelSelection(String modelName) {
        PlayerModelSelectionSyncService.getInstance().syncModelSelection(modelName);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ClientRenderRuntime.get().modelRepository().reloadSubject(ModelRequestKey.player(minecraft.player, modelName));
    }
}
