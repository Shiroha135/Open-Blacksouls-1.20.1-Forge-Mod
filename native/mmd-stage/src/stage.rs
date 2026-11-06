use glam::{Mat4, Quat, Vec3};

use crate::physics::bullet_ffi::{BulletRigidBody, BulletShape, BulletWorld, RigidBodyInfo};

pub const MAX_SCENE_BOXES: usize = 192;
pub const SCENE_COLLISION_GROUP: i32 = 1 << 14;

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct SceneBox {
    pub min: Vec3,
    pub max: Vec3,
}

impl SceneBox {
    pub fn parse(values: &[f32]) -> Option<Self> {
        if values.len() != 6 || values.iter().any(|value| !value.is_finite() || value.abs() > 32.0) {
            return None;
        }
        let min = Vec3::new(values[0], values[1], values[2]);
        let max = Vec3::new(values[3], values[4], values[5]);
        let size = max - min;
        if size.min_element() <= 0.00001 || size.max_element() > 32.0 {
            return None;
        }
        Some(Self { min, max })
    }

    pub fn local_transform(self, yaw: f32, scale: f32) -> (Mat4, Vec3) {
        let center = (self.min + self.max) * (0.5 / scale);
        let right = Quat::from_rotation_y(yaw) * center;
        let left = Vec3::new(right.x, right.y, -right.z);
        (
            Mat4::from_rotation_translation(Quat::from_rotation_y(-yaw), left),
            (self.max - self.min) * (0.5 / scale),
        )
    }
}

struct SceneBody {
    body: BulletRigidBody,
    _shape: BulletShape,
    half: Vec3,
}

#[derive(Default)]
pub struct StageEnvironment {
    bodies: Vec<SceneBody>,
}

impl StageEnvironment {
    pub fn sync(&mut self, world: &BulletWorld, boxes: &[SceneBox], yaw: f32, scale: f32) -> bool {
        if boxes.len() > MAX_SCENE_BOXES || !yaw.is_finite() || !scale.is_finite() || !(0.001..=10.0).contains(&scale) {
            return false;
        }
        let transforms: Vec<_> = boxes.iter().map(|value| value.local_transform(yaw, scale)).collect();
        if transforms.len() == self.bodies.len()
            && transforms.iter().zip(&self.bodies).all(|((_, half), body)| half.abs_diff_eq(body.half, 0.00001))
        {
            for ((transform, _), body) in transforms.iter().zip(&self.bodies) {
                body.body.set_transform(*transform);
            }
            return true;
        }
        let mut replacement = Vec::with_capacity(boxes.len());
        for (transform, half) in transforms {
            let Some(shape) = BulletShape::r#box(half.x, half.y, half.z) else { return false };
            let info = RigidBodyInfo {
                mass: 0.0,
                linear_damping: 0.0,
                angular_damping: 0.0,
                friction: 0.6,
                restitution: 0.0,
                additional_damping: false,
                is_kinematic: false,
                disable_deactivation: false,
                no_contact_response: false,
                initial_transform: transform,
            };
            let Some(body) = BulletRigidBody::new(&info, &shape) else { return false };
            body.mark_stage();
            replacement.push(SceneBody { body, _shape: shape, half });
        }
        self.clear(world);
        self.bodies = replacement;
        for value in &self.bodies {
            world.add_rigid_body(&value.body, SCENE_COLLISION_GROUP, 0xffff);
        }
        true
    }

    pub fn clear(&mut self, world: &BulletWorld) {
        for value in &self.bodies {
            world.remove_rigid_body(&value.body);
        }
        self.bodies.clear();
    }

    pub fn len(&self) -> usize {
        self.bodies.len()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rejects_nonfinite_reversed_and_unbounded_shapes() {
        assert!(SceneBox::parse(&[0.0, 0.0, 0.0, 1.0, 1.0, 1.0]).is_some());
        for values in [[f32::NAN, 0.0, 0.0, 1.0, 1.0, 1.0], [0.0, 0.0, 0.0, -1.0, 1.0, 1.0], [0.0, 0.0, 0.0, 100.0, 1.0, 1.0]] {
            assert!(SceneBox::parse(&values).is_none());
        }
    }

    #[test]
    fn local_box_matches_rendered_handedness_yaw_and_scale() {
        let value = SceneBox::parse(&[0.0, -1.0, 1.0, 2.0, 0.0, 3.0]).unwrap();
        let (transform, half) = value.local_transform(std::f32::consts::FRAC_PI_2, 0.1);
        assert!(half.abs_diff_eq(Vec3::new(10.0, 5.0, 10.0), 0.0001));
        assert!(transform.w_axis.truncate().abs_diff_eq(Vec3::new(20.0, -5.0, 10.0), 0.0001));
    }

    #[test]
    fn world_contact_stops_dynamic_body_and_cleans_up() {
        let world = BulletWorld::new(0.0, -9.8, 0.0).unwrap();
        let mut environment = StageEnvironment::default();
        let floor = SceneBox::parse(&[-2.0, -0.1, -2.0, 2.0, 0.0, 2.0]).unwrap();
        assert!(environment.sync(&world, &[floor], 0.0, 1.0));
        let shape = BulletShape::sphere(0.1).unwrap();
        let info = RigidBodyInfo {
            mass: 1.0, linear_damping: 0.1, angular_damping: 0.1, friction: 0.5, restitution: 0.0,
            additional_damping: false, is_kinematic: false, disable_deactivation: true,
            no_contact_response: false, initial_transform: Mat4::from_translation(Vec3::new(0.0, 1.0, 0.0)),
        };
        let body = BulletRigidBody::new(&info, &shape).unwrap();
        world.add_rigid_body(&body, 1, SCENE_COLLISION_GROUP);
        for _ in 0..240 { world.step(1.0 / 120.0, 1, 1.0 / 120.0); }
        assert!(body.get_position().y > 0.085, "scene floor must stop PMX dynamic bodies");
        assert_eq!(environment.len(), 1);
        assert!(environment.sync(&world, &[], 0.0, 1.0));
        for _ in 0..120 { world.step(1.0 / 120.0, 1, 1.0 / 120.0); }
        assert!(body.get_position().y < -1.0, "removing scene shapes must remove their physical contacts");
        world.remove_rigid_body(&body);
        environment.clear(&world);
    }
}
