package yuki135.blacksouls.item;

import yuki135.blacksouls.block.OriginalBlockDefinition;
import yuki135.blacksouls.block.OriginalTileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class OriginalTileBlockItem extends BlockItem {
    private final OriginalBlockDefinition definition;

    public OriginalTileBlockItem(OriginalBlockDefinition definition, Properties properties) {
        super(definition.blocks().get(0).get(), properties);
        this.definition = definition;
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!super.canPlace(context, state)) return false;
        var level = context.getLevel();
        var player = context.getPlayer();
        Direction facing = state.getValue(OriginalTileBlock.FACING);
        for (int index = 0; index < definition.parts().size(); index++) {
            BlockPos target = context.getClickedPos().offset(OriginalBlockDefinition.rotate(definition.parts().get(index).offset(), facing));
            if (level.isOutsideBuildHeight(target) || !level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target)) return false;
            var partContext = BlockPlaceContext.at(context, target, context.getClickedFace());
            if (!level.getBlockState(target).canBeReplaced(partContext)) return false;
            if (player != null && (!level.mayInteract(player, target) || !player.mayUseItemAt(target, context.getClickedFace(), context.getItemInHand()))) return false;
            BlockState part = definition.blocks().get(index).get().defaultBlockState().setValue(OriginalTileBlock.FACING, facing);
            CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
            if (!level.isUnobstructed(part, target, collision)) return false;
        }
        return true;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (!super.placeBlock(context, state)) return false;
        Direction facing = state.getValue(OriginalTileBlock.FACING);
        for (int index = 1; index < definition.parts().size(); index++) {
            BlockPos target = context.getClickedPos().offset(OriginalBlockDefinition.rotate(definition.parts().get(index).offset(), facing));
            BlockState part = definition.blocks().get(index).get().defaultBlockState().setValue(OriginalTileBlock.FACING, facing);
            if (!context.getLevel().setBlock(target, part, Block.UPDATE_ALL)) {
                context.getLevel().removeBlock(context.getClickedPos(), false);
                return false;
            }
        }
        return true;
    }
}
