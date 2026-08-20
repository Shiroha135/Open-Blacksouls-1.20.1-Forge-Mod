package com.shiroha.mmdskin.player.render;

import com.shiroha.mmdskin.model.runtime.ManagedModel;
import com.shiroha.mmdskin.model.runtime.ModelRenderProperties;
import com.shiroha.mmdskin.player.runtime.FirstPersonManager;
import com.shiroha.mmdskin.render.scene.MutableRenderPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** 文件职责：集中计算玩家模型渲染姿态与模型属性读取。 */
@SuppressWarnings("removal")
public final class PlayerRenderHelper {

    private static final ResourceLocation DECOCRAFT_SEAT = new ResourceLocation("decocraft", "seat");
    private static final float DECOCRAFT_SEAT_Y_OFFSET = -0.30f;

    private PlayerRenderHelper() {}

    public static MutableRenderPose calculateMutableRenderPose(AbstractClientPlayer player, ManagedModel modelData, float tickDelta) {
        MutableRenderPose params = new MutableRenderPose();
        ModelRenderProperties renderProperties = modelData.renderProperties();
        float vrBodyYaw = FirstPersonManager.vrRuntime().getBodyYawDegrees(player, tickDelta);
        params.bodyYaw = Float.isFinite(vrBodyYaw) ? vrBodyYaw : player.yBodyRot;
        params.bodyPitch = 0.0f;
        params.translation.zero();

        if (player.isFallFlying()) {
            params.bodyPitch = player.getXRot() + renderProperties.flyingPitch();
            params.translation.set(renderProperties.flyingTranslation());
        } else if (player.isSleeping()) {
            params.bodyYaw = player.getBedOrientation().toYRot() + 180.0f;
            params.bodyPitch = renderProperties.sleepingPitch();
            params.translation.set(renderProperties.sleepingTranslation());
        } else if (player.isSwimming()) {
            params.bodyPitch = player.getXRot() + renderProperties.swimmingPitch();
            params.translation.set(renderProperties.swimmingTranslation());
        } else if (player.isVisuallyCrawling()) {
            params.bodyPitch = renderProperties.crawlingPitch();
            params.translation.set(renderProperties.crawlingTranslation());
        }

        var vehicle = player.getVehicle();
        if (vehicle != null && DECOCRAFT_SEAT.equals(BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()))) {
            params.translation.add(0.0f, DECOCRAFT_SEAT_Y_OFFSET, 0.0f);
        }

        return params;
    }

    public static float[] getModelSize(ManagedModel modelData) {
        return new float[] {
                modelData.renderProperties().modelScale(),
                modelData.renderProperties().inventoryScale()
        };
    }
}
