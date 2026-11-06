use crate::pmx::types::VertexIndex;

#[derive(Debug, Clone)]
pub struct DisplayFrameElement<I: VertexIndex> {
  pub target_type: u8,
  pub target_index: I,
}

#[derive(Debug, Clone)]
pub struct DisplayFrame<I: VertexIndex> {
  pub local_name: String,
  pub universal_name: String,
  pub special_flag: u8,
  pub elements: Vec<DisplayFrameElement<I>>,
}
