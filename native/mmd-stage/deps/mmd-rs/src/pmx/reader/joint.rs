use crate::pmx::joint::{Joint, JointType};
use crate::pmx::reader::{helpers::ReadHelpers, RigidBodyReader};
use crate::{Config, Error, Settings};
use byteorder::{ReadBytesExt, LE};
use std::io::Read;

pub struct JointReader<R> {
  pub settings: Settings,
  pub count: i32,
  pub remaining: i32,
  pub(crate) read: R,
  pub(crate) poison: bool,
}

impl<R: Read> JointReader<R> {
  pub fn new(mut previous: RigidBodyReader<R>) -> Result<Self, Error> {
    assert!(!previous.poison);
    while previous.remaining > 0 {
      previous.next::<crate::DefaultConfig>()?;
    }
    // This is problematic. BoneReader drains MaterialReader. MaterialReader doesn't need Header in next().
    // RigidBodyReader needs headers.

    let count = previous.read.read_i32::<LE>()?;
    Ok(Self {
      settings: previous.settings,
      count,
      remaining: count,
      read: previous.read,
      poison: false,
    })
  }

  // I will redefine new to take the underlying reader directly if chaining is hard,
  // OR just duplicate logic.
  // Better: Allow users to create JointReader from RigidBodyReader manually after they finished reading bodies.
  // The `new` helper in other readers assumes you pass the previous reader to consume it.
  // Use `into_inner()` pattern?
  // `mmd-rs` pattern seems to be: pass prev reader, drain it.
  // But if next() requires arguments, this pattern breaks unless arguments are passed or stored.
  // RigidBodyReader stores settings, but next() asks for HeaderReader mainly for convenience?
  // Actually `HeaderReader` was passed in my implementation of RigidBodyReader::next.
  // Why? `read_index` needs sizes. `read_text` needs encoding.
  // These are in `settings`. `RigidBodyReader` HAS `settings`.
  // So `RigidBodyReader::next` should NOT need `&HeaderReader`. It should use `self.settings`.

  pub fn next<C: Config>(&mut self) -> Result<Option<Joint>, Error> {
    assert!(!self.poison);
    let result = self.next_impl::<C>();
    if result.is_err() {
      self.poison = true;
    }
    result
  }

  fn next_impl<C: Config>(&mut self) -> Result<Option<Joint>, Error> {
    if self.remaining <= 0 {
      return Ok(None);
    }
    self.remaining -= 1;

    let local_name = self.read.read_text(self.settings.text_encoding)?;
    let universal_name = self.read.read_text(self.settings.text_encoding)?;

    let type_ = match self.read.read_u8()? {
      0 => JointType::Spring6DOF,
      1 => JointType::SixDof,
      2 => JointType::P2p,
      3 => JointType::ConeTwist,
      4 => JointType::Slider,
      5 => JointType::Hinge,
      _ => return Err(Error::Data("Invalid joint type".into())),
    };

    let rigid_body_a_index = self.read.read_index(self.settings.rigidbody_index_size)?;
    let rigid_body_b_index = self.read.read_index(self.settings.rigidbody_index_size)?;

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

    let position_min = [
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
    ];
    let position_max = [
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
    ];
    let rotation_min = [
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
    ];
    let rotation_max = [
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
    ];

    let position_spring = [
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
    ];
    let rotation_spring = [
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
      self.read.read_f32::<LE>()?,
    ];

    Ok(Some(Joint {
      local_name,
      universal_name,
      type_,
      rigid_body_a_index,
      rigid_body_b_index,
      position,
      rotation,
      position_min,
      position_max,
      rotation_min,
      rotation_max,
      position_spring,
      rotation_spring,
    }))
  }
}
