use crate::pmx::reader::{helpers::ReadHelpers, DisplayFrameReader};
use crate::pmx::rigid_body::{RigidBody, RigidBodyMode, RigidBodyShape};
use crate::{Config, Error, Settings};
use byteorder::{ReadBytesExt, LE};
use std::io::Read;

pub struct RigidBodyReader<R> {
  pub settings: Settings,
  pub count: i32,
  pub remaining: i32,
  pub(crate) read: R,
  pub(crate) poison: bool,
}

impl<R: Read> RigidBodyReader<R> {
  pub fn new(mut previous: DisplayFrameReader<R>) -> Result<Self, Error> {
    assert!(!previous.poison);
    while previous.remaining > 0 {
      previous.next::<crate::DefaultConfig>()?;
    }
    let count = previous.read.read_i32::<LE>()?;
    Ok(Self {
      settings: previous.settings,
      count,
      remaining: count,
      read: previous.read,
      poison: false,
    })
  }

  pub fn next<C: Config>(&mut self) -> Result<Option<RigidBody>, Error> {
    assert!(!self.poison);
    let result = self.next_impl::<C>();
    if result.is_err() {
      self.poison = true;
    }
    result
  }

  fn next_impl<C: Config>(&mut self) -> Result<Option<RigidBody>, Error> {
    if self.remaining <= 0 {
      return Ok(None);
    }
    self.remaining -= 1;

    let local_name = self.read.read_text(self.settings.text_encoding)?;
    let universal_name = self.read.read_text(self.settings.text_encoding)?;

    let bone_index = self.read.read_index(self.settings.bone_index_size)?;
    let group = self.read.read_u8()?;
    let un_collision_group_flag = self.read.read_u16::<LE>()?;
    let shape = match self.read.read_u8()? {
      0 => RigidBodyShape::Sphere,
      1 => RigidBodyShape::Box,
      2 => RigidBodyShape::Capsule,
      _ => return Err(Error::Data("Invalid rigid body shape".into())),
    };

    let size = [
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
    ];
    let position = [
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
    ];
    let rotation = [
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
    ];

    let mass = self.read.read_f32::<LE>()?;
    let move_attenuation = self.read.read_f32::<LE>()?;
    let rotation_attenuation = self.read.read_f32::<LE>()?;
    let repulsion = self.read.read_f32::<LE>()?;
    let friction = self.read.read_f32::<LE>()?;

    let mode = match self.read.read_u8()? {
      0 => RigidBodyMode::Static,
      1 => RigidBodyMode::Dynamic,
      2 => RigidBodyMode::DynamicWithBonePosition,
      _ => return Err(Error::Data("Invalid rigid body mode".into())),
    };

    Ok(Some(RigidBody {
      local_name,
      universal_name,
      bone_index,
      group,
      un_collision_group_flag,
      shape,
      size,
      position,
      rotation,
      mass,
      move_attenuation,
      rotation_attenuation,
      repulsion,
      friction,
      mode,
    }))
  }
}
