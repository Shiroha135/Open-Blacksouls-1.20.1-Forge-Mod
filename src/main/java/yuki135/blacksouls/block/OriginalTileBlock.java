package yuki135.blacksouls.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OriginalTileBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final OriginalBlockDefinition definition;
    private final int part;

    public OriginalTileBlock(OriginalBlockDefinition definition, int part) {
        super(properties(definition));
        this.definition = definition;
        this.part = part;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static BlockBehaviour.Properties properties(OriginalBlockDefinition definition) {
        SoundType sound = switch (definition.material()) {
            case "stone" -> SoundType.STONE;
            case "metal", "gold" -> SoundType.METAL;
            case "cloth" -> SoundType.WOOL;
            case "plant" -> SoundType.GRASS;
            case "glass" -> SoundType.GLASS;
            default -> SoundType.WOOD;
        };
        MapColor color = switch (definition.material()) {
            case "stone", "metal" -> MapColor.STONE;
            case "cloth" -> MapColor.COLOR_RED;
            case "gold" -> MapColor.GOLD;
            case "plant" -> MapColor.PLANT;
            default -> MapColor.WOOD;
        };
        var properties = BlockBehaviour.Properties.of().mapColor(color).sound(sound).strength(definition.strength())
                .lightLevel(state -> definition.light());
        return definition.solid() ? properties : properties.noOcclusion();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return definition.parts().get(part).shape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(definition.blocks().get(0).get().asItem());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (replacement.is(this) || level.isClientSide || moving) return;
        Direction facing = state.getValue(FACING);
        BlockPos origin = pos.subtract(OriginalBlockDefinition.rotate(definition.parts().get(part).offset(), facing));
        for (int index = 0; index < definition.parts().size(); index++) {
            if (index == part) continue;
            BlockPos target = origin.offset(OriginalBlockDefinition.rotate(definition.parts().get(index).offset(), facing));
            if (!level.hasChunkAt(target)) continue;
            BlockState other = level.getBlockState(target);
            if (other.is(definition.blocks().get(index).get()) && other.getValue(FACING) == facing) {
                level.setBlock(target, other.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            }
        }
    }

    public OriginalBlockDefinition definition() { return definition; }
}
