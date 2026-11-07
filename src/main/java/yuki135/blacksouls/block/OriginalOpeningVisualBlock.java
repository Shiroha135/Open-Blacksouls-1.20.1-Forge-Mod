package yuki135.blacksouls.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OriginalOpeningVisualBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final OriginalOpeningVisualDefinition definition;
    private final int part;
    private final BlockState[] states=new BlockState[4];

    public OriginalOpeningVisualBlock(OriginalOpeningVisualDefinition definition, int part) {
        super(properties(definition));
        definition.partId(part);
        this.definition=definition;
        this.part=part;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
        for (Direction facing:Direction.Plane.HORIZONTAL) states[facing.get2DDataValue()]=defaultBlockState().setValue(FACING,facing);
    }

    private static BlockBehaviour.Properties properties(OriginalOpeningVisualDefinition definition) {
        var properties=BlockBehaviour.Properties.of().strength(definition.strength()).noLootTable().lightLevel(state->definition.light());
        switch (definition.material()) {
            case "plant" -> properties.mapColor(MapColor.PLANT).sound(SoundType.GRASS);
            case "stone" -> properties.mapColor(MapColor.STONE).sound(SoundType.STONE);
            default -> properties.mapColor(MapColor.WOOD).sound(SoundType.WOOD);
        }
        return definition.occluding()?properties:properties.noOcclusion();
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) { return definition.parts().get(part).shape(state.getValue(FACING)); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) { return getShape(state,level,position,context); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }

    public OriginalOpeningVisualDefinition definition() { return definition; }
    public int part() { return part; }
    public boolean hasCollision() { return definition.parts().get(part).hasCollision(); }
    public BlockState state(Direction facing) { OriginalOpeningVisualDefinition.horizontal(facing);return states[facing.get2DDataValue()]; }
}
