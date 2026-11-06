package yuki135.mmdskin.render.pipeline;

import yuki135.mmdskin.bridge.runtime.NativeScenePort;
import yuki135.mmdskin.render.scene.RenderScene;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 文件职责：同步生物实体到模型运行时的姿态状态。 */
public final class LivingEntityModelStateHelper {
    private static final NativeScenePort NOOP_SCENE_PORT = new NativeScenePort() {
        @Override
        public void setHeadAngle(long modelHandle, float x, float y, float z, boolean worldSpace) {
        }

        @Override
        public void setModelPositionAndYaw(long modelHandle, float x, float y, float z, float yawRadians) {
        }

        @Override
        public void setAutoBlinkEnabled(long modelHandle, boolean enabled) {
        }

        @Override
        public void setEyeTrackingEnabled(long modelHandle, boolean enabled) {
        }

        @Override
        public void setEyeMaxAngle(long modelHandle, float maxAngle) {
        }

        @Override
        public void setEyeAngle(long modelHandle, float eyeX, float eyeY) {
        }
    };

    private static final float MODEL_SCALE = 0.09f;
    private static volatile NativeScenePort scenePort = NOOP_SCENE_PORT;

    private LivingEntityModelStateHelper() {
    }

    public static void configureRuntimeCollaborators(NativeScenePort scenePort) {
        LivingEntityModelStateHelper.scenePort = scenePort != null ? scenePort : NOOP_SCENE_PORT;
    }

    public static void syncModelState(long modelHandle, LivingEntity entity, float entityYaw,
                                      float tickDelta, RenderScene context, String modelName) {
        HeadAngleHelper.updateHeadAngle(scenePort, modelHandle, entity, entityYaw, tickDelta, context);
        EyeTrackingHelper.updateEyeTracking(scenePort, modelHandle, entity, entityYaw, tickDelta, modelName);
        if (!yuki135.blacksouls.client.stage.StagePhysicsRuntime.ownsRoot(modelHandle, entity)) {
            Vec3 origin = new Vec3(Mth.lerp(tickDelta, entity.xo, entity.getX()),
                    Mth.lerp(tickDelta, entity.yo, entity.getY()), Mth.lerp(tickDelta, entity.zo, entity.getZ()));
            scenePort.setModelPositionAndYaw(modelHandle, (float) (origin.x / MODEL_SCALE),
                    (float) (origin.y / MODEL_SCALE), (float) (origin.z / MODEL_SCALE), entityYaw * Mth.DEG_TO_RAD);
        }
    }
}
