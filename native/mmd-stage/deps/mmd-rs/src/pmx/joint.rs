#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum JointType {
  Spring6DOF = 0,
  SixDof = 1,
  P2p = 2,
  ConeTwist = 3,
  Slider = 4,
  Hinge = 5,
}

#[derive(Debug, Clone)]
pub struct Joint {
  pub local_name: String,
  pub universal_name: String,
  pub type_: JointType,
  pub rigid_body_a_index: i32,
  pub rigid_body_b_index: i32,
  pub position: [f32; 3],
  pub rotation: [f32; 3],
  pub position_min: [f32; 3],
  pub position_max: [f32; 3],
  pub rotation_min: [f32; 3],
  pub rotation_max: [f32; 3],
  pub position_spring: [f32; 3],
  pub rotation_spring: [f32; 3],
}
