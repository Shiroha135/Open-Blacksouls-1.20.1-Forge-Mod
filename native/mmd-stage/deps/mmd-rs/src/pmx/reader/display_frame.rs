use crate::pmx::display_frame::{DisplayFrame, DisplayFrameElement};
use crate::pmx::reader::{helpers::ReadHelpers, MorphReader};
use crate::{Config, Error, Settings};
use byteorder::{ReadBytesExt, LE};
use std::convert::TryFrom;
use std::io::Read;

pub struct DisplayFrameReader<R> {
  pub settings: Settings,
  pub count: i32,
  pub remaining: i32,
  pub(crate) read: R,
  pub(crate) poison: bool,
}

impl<R: Read> DisplayFrameReader<R> {
  pub fn new(mut m: MorphReader<R>) -> Result<Self, Error> {
    assert!(!m.poison);
    while m.remaining > 0 {
      m.next::<crate::DefaultConfig>()?;
    }
    let count = m.read.read_i32::<LE>()?;
    Ok(Self {
      settings: m.settings,
      count,
      remaining: count,
      read: m.read,
      poison: false,
    })
  }

  pub fn next<C: Config>(&mut self) -> Result<Option<DisplayFrame<C::VertexIndex>>, Error> {
    assert!(!self.poison);
    let result = self.next_impl::<C>();
    if result.is_err() {
      self.poison = true;
    }
    result
  }

  fn next_impl<C: Config>(&mut self) -> Result<Option<DisplayFrame<C::VertexIndex>>, Error> {
    if self.remaining <= 0 {
      return Ok(None);
    }
    self.remaining -= 1;

    let local_name = self.read.read_text(self.settings.text_encoding)?;
    let universal_name = self.read.read_text(self.settings.text_encoding)?;
    let special_flag = self.read.read_u8()?;
    let element_count = self.read.read_i32::<LE>()?;

    let mut elements = Vec::with_capacity(element_count as usize);
    for _ in 0..element_count {
      let target_type = self.read.read_u8()?;
      let target_index = if target_type == 0 {
        // Bone Index
        let idx = self.read.read_index::<i32>(self.settings.bone_index_size)?;
        C::VertexIndex::try_from(idx).map_err(|_| Error::Data("Index conversion error".into()))?
      } else {
        // Morph Index
        let idx = self
          .read
          .read_index::<i32>(self.settings.morph_index_size)?;
        C::VertexIndex::try_from(idx).map_err(|_| Error::Data("Index conversion error".into()))?
      };
      elements.push(DisplayFrameElement {
        target_type,
        target_index,
      });
    }

    Ok(Some(DisplayFrame {
      local_name,
      universal_name,
      special_flag,
      elements,
    }))
  }
}
