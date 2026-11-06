#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RigidBodyShape {
  Sphere = 0,
  Box = 1,
  Capsule = 2,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RigidBodyMode {
  Static = 0,
  Dynamic = 1,
  DynamicWithBonePosition = 2,
}

#[derive(Debug, Clone)]
pub struct RigidBody {
  pub local_name: String,
  pub universal_name: String,
  pub bone_index: i32,
  pub group: u8,
  pub un_collision_group_flag: u16,
  pub shape: RigidBodyShape,
  pub size: [f32; 3],
  pub position: [f32; 3],
  pub rotation: [f32; 3],
  pub mass: f32,
  pub move_attenuation: f32,
  pub rotation_attenuation: f32,
  pub repulsion: f32,
  pub friction: f32,
  pub mode: RigidBodyMode,
}
