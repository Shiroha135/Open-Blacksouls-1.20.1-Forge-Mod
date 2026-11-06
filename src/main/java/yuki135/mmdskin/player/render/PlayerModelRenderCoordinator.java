/* 文件职责：协调玩家模型在普通视角、第一人称与 VR 场景中的渲染切换。 */
package yuki135.mmdskin.player.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import yuki135.mmdskin.config.ModelConfigManager;
import yuki135.mmdskin.config.RuntimeConfigPortHolder;
import yuki135.mmdskin.config.ModelConfigData;
import yuki135.mmdskin.model.runtime.ManagedModel;
import yuki135.mmdskin.player.animation.AnimationStateManager;
import yuki135.mmdskin.player.animation.PendingAnimSignalCache;
import yuki135.mmdskin.player.port.VrRuntimePort;
import yuki135.mmdskin.player.runtime.FirstPersonManager;
import yuki135.mmdskin.player.runtime.MmdSkinRendererPlayerHelper;
import yuki135.mmdskin.model.runtime.ModelInstance;
import yuki135.mmdskin.render.scene.RenderScene;
import yuki135.mmdskin.render.scene.MutableRenderPose;
import yuki135.mmdskin.render.backend.BaseModelInstance;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

/** 文件职责：协调玩家模型在普通视角、第一人称与 VR 场景中的渲染切换。 */
final class PlayerModelRenderCoordinator {

    private PlayerModelRenderCoordinator() {
    }

    static PlayerRenderAction render(PlayerRenderSelection selection,
                                                    AbstractClientPlayer player,
                                                   float entityYaw,
                                                   float tickDelta,
                                                   PoseStack matrixStack,
                                                   MultiBufferSource vertexConsumers,
                                                   int packedLight,
                                                   ManagedModel modelData) {
        ModelInstance model = modelData.modelInstance();
        VrRuntimePort vrRuntime = FirstPersonManager.vrRuntime();

        float[] size = PlayerRenderHelper.getModelSize(modelData);
        boolean isVr = selection.isLocalPlayer() && vrRuntime.isLocalPlayerInVr();
        syncVrState(modelData, player, tickDelta, isVr, vrRuntime);

        ModelConfigData modelConfig = ModelConfigManager.getConfig(selection.selectedModel());
        float combinedScale = size[0] * modelConfig.modelScale;
        long modelHandle = model.getModelHandle();
        if (selection.isLocalPlayer()) {
            FirstPersonManager.preRender(modelHandle, combinedScale, true);
        }
        boolean isFirstPerson = !isVr && selection.isLocalPlayer() && FirstPersonManager.isActive();

        if (!isVr) {
            AnimationStateManager.updateAnimationState(player, modelData);
        }
        consumePendingSignals(player, modelData, selection.isLocalPlayer());

        MutableRenderPose params = PlayerRenderHelper.calculateMutableRenderPose(player, modelData, tickDelta);
        boolean needsPostRenderSync = selection.isLocalPlayer();

        matrixStack.pushPose();
        try {
            if (InventoryRenderHelper.isInventoryScreen()) {
                InventoryRenderHelper.renderInInventory(player, model, entityYaw, tickDelta, matrixStack, packedLight, size);
            } else {
                matrixStack.scale(size[0], size[0], size[0]);
                RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
                RenderScene context = isFirstPerson ? RenderScene.FIRST_PERSON : RenderScene.WORLD;
                model.render(player, params.bodyYaw, params.bodyPitch, params.translation, tickDelta, matrixStack, packedLight, context);
            }

            if (needsPostRenderSync) {
                FirstPersonManager.postRender(modelHandle, player, tickDelta);
                needsPostRenderSync = false;
            }

            ItemRenderHelper.renderItems(
                    player,
                    modelData,
                    matrixStack,
                    vertexConsumers,
                    packedLight,
                    modelConfig.heldItemScale);
            return PlayerRenderAction.CANCEL;
        } finally {
            try {
                if (needsPostRenderSync) {
                    FirstPersonManager.postRender(modelHandle, player, tickDelta);
                }
            } finally {
                matrixStack.popPose();
            }
        }
    }

    private static void syncVrState(ManagedModel modelData,
                                    AbstractClientPlayer player,
                                    float tickDelta,
                                    boolean isVr,
                                    VrRuntimePort vrRuntime) {
        ModelInstance model = modelData.modelInstance();
        if (!(model instanceof BaseModelInstance abstractModel)) {
            return;
        }

        if (isVr) {
            vrRuntime.applyMmdRenderState(true);
            if (!abstractModel.isVrActive()) {
                MmdSkinRendererPlayerHelper.suppressDefaultAnimationState(modelData);
                vrRuntime.setModelVrEnabled(model.getModelHandle(), true);
                abstractModel.setVrActive(true);
            }
            vrRuntime.updateModelVr(model.getModelHandle(), player, tickDelta, RuntimeConfigPortHolder.get().getVrArmIkStrength());
            return;
        }

        vrRuntime.applyMmdRenderState(false);
        if (abstractModel.isVrActive()) {
            vrRuntime.setModelVrEnabled(model.getModelHandle(), false);
            abstractModel.setVrActive(false);
            MmdSkinRendererPlayerHelper.resetModelAnimationState(player, modelData);
        }
    }

    private static void consumePendingSignals(AbstractClientPlayer player,
                                              ManagedModel modelData,
                                              boolean isLocalPlayer) {
        if (isLocalPlayer) {
            return;
        }

        PendingAnimSignalCache.SignalType signal = PendingAnimSignalCache.consume(player.getUUID());
        if (signal == PendingAnimSignalCache.SignalType.RESET) {
            MmdSkinRendererPlayerHelper.ResetPhysics(player);
        }
    }
}
