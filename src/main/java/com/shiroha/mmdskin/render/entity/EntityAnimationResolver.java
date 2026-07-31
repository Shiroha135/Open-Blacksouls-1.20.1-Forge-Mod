package com.shiroha.mmdskin.render.entity;

import com.shiroha.mmdskin.model.runtime.ManagedModel;
import com.shiroha.mmdskin.player.runtime.EntityAnimState;
import com.shiroha.mmdskin.render.scene.MutableRenderPose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * 通用实体动画状态解析器。
 */
public final class EntityAnimationResolver {

    private EntityAnimationResolver() {
    }

    public static void resolve(Entity entity, ManagedModel model,
                                float entityYaw, float tickDelta, MutableRenderPose params) {

        if (entity instanceof LivingEntity living) {
            params.bodyYaw = Mth.rotLerp(tickDelta, living.yBodyRotO, living.yBodyRot);
        } else {
            params.bodyYaw = entityYaw;
        }
        params.bodyPitch = 0.0f;
        params.translation.zero();

        if (entity instanceof CustomEntityAnimationProvider provider) {
            String animation = provider.getMmdAnimation();
            if (animation != null && !animation.isBlank()
                    && changeCustomAnimationOnce(model, animation, 0)) {
                return;
            }
            if (model.entityState().playCustomAnim) {
                model.entityState().playCustomAnim = false;
                model.entityState().playStageAnim = false;
                model.entityState().invalidateStateLayers();
                model.modelInstance().resetPhysics();
            }
        }

        if (entity instanceof LivingEntity living) {
            if (living.getHealth() <= 0.0f) {
                changeAnimOnce(model, EntityAnimState.State.Die, 0);
                return;
            }
            if (living.isSleeping()) {
                params.bodyYaw = living.getBedOrientation().toYRot() + 180.0f;
                params.bodyPitch = model.renderProperties().sleepingPitch();
                params.translation.set(model.renderProperties().sleepingTranslation());
                changeAnimOnce(model, EntityAnimState.State.Sleep, 0);
                return;
            }
        }

        boolean hasMovement = entity.getX() - entity.xo != 0.0f
                           || entity.getZ() - entity.zo != 0.0f;

        if (entity.isVehicle() && hasMovement) {
            changeAnimOnce(model, EntityAnimState.State.Driven, 0);
        } else if (entity.isVehicle()) {
            changeAnimOnce(model, EntityAnimState.State.Ridden, 0);
        } else if (entity.isSwimming()) {
            changeAnimOnce(model, EntityAnimState.State.Swim, 0);
        } else if (hasMovement && entity.getVehicle() == null) {
            changeAnimOnce(model, EntityAnimState.State.Walk, 0);
        } else {
            changeAnimOnce(model, EntityAnimState.State.Idle, 0);
        }
    }

    private static boolean changeCustomAnimationOnce(ManagedModel model, String animation, int layer) {
        if (model.entityState().playCustomAnim
                && Objects.equals(model.entityState().layerAnimationKeys[layer], animation)) {
            return true;
        }
        long handle = model.animationLibrary().animation(animation);
        if (handle == 0L) {
            return false;
        }
        model.entityState().playCustomAnim = true;
        model.entityState().playStageAnim = false;
        model.entityState().invalidateStateLayers();
        model.entityState().layerAnimationKeys[layer] = animation;
        model.modelInstance().setLayerLoop(layer, true);
        model.modelInstance().transitionAnim(handle, layer, 0.25f);
        model.modelInstance().changeAnim(0L, 1);
        model.modelInstance().changeAnim(0L, 2);
        model.modelInstance().resetPhysics();
        return true;
    }

    private static void changeAnimOnce(ManagedModel model,
                                        EntityAnimState.State targetState, int layer) {
        if (model.entityState().stateLayers[layer] != targetState) {
            model.entityState().stateLayers[layer] = targetState;
            String property = EntityAnimState.getPropertyName(targetState);
            model.modelInstance().changeAnim(model.animationLibrary().animation(property), layer);
        }
    }
}
