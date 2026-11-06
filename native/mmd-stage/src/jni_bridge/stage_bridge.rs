use glam::DVec3;
use jni::objects::{JClass, JFloatArray};
use jni::sys::{jboolean, jdouble, jfloat, jint, jlong};
use jni::JNIEnv;

use super::MODELS;
use crate::stage::{SceneBox, MAX_SCENE_BOXES};

#[no_mangle]
pub extern "system" fn Java_com_BlackSouls_BlackSoulsMod_client_stage_StageNativeBridge_GetApiVersion(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    1
}

#[no_mangle]
pub extern "system" fn Java_com_BlackSouls_BlackSoulsMod_client_stage_StageNativeBridge_SyncScene(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    x: jdouble,
    y: jdouble,
    z: jdouble,
    yaw: jfloat,
    scale: jfloat,
    boxes: JFloatArray,
    count: jint,
    reset: jboolean,
) -> jboolean {
    if count < 0 || count as usize > MAX_SCENE_BOXES || !x.is_finite() || !y.is_finite()
        || !z.is_finite() || !yaw.is_finite() || !scale.is_finite() || !(0.001..=10.0).contains(&scale) {
        return 0;
    }
    let length = count as usize * 6;
    let Ok(capacity) = env.get_array_length(&boxes) else { return 0 };
    if capacity as usize != length { return 0; }
    let mut data = vec![0.0f32; length];
    if env.get_float_array_region(&boxes, 0, &mut data).is_err() { return 0; }
    let Some(shapes): Option<Vec<SceneBox>> = data.chunks_exact(6).map(SceneBox::parse).collect() else { return 0 };
    let Ok(models) = MODELS.read() else { return 0 };
    let Some(model) = models.get(&handle) else { return 0 };
    let Ok(mut model) = model.lock() else { return 0 };
    model.sync_stage_scene(DVec3::new(x, y, z), yaw, scale, &shapes, reset != 0) as jboolean
}

#[no_mangle]
pub extern "system" fn Java_com_BlackSouls_BlackSoulsMod_client_stage_StageNativeBridge_GetSceneColliderCount(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    let Ok(models) = MODELS.read() else { return -1 };
    let Some(model) = models.get(&handle) else { return -1 };
    let Ok(model) = model.lock() else { return -1 };
    model.stage_collider_count() as jint
}
