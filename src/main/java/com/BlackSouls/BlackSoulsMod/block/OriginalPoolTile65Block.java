package com.BlackSouls.BlackSoulsMod.block;

import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalPoolCheshirePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OriginalPoolTile65Block extends Block {
    private static final VoxelShape SHAPE = Block.box(0, -16, 0, 16, 24, 16);

    public OriginalPoolTile65Block() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0F, 3600000.0F)
                .sound(SoundType.WOOD).noLootTable().noOcclusion()
                .isSuffocating((state, level, position) -> false)
                .isViewBlocking((state, level, position) -> false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos position, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return player instanceof ServerPlayer serverPlayer
                && OriginalPoolCheshirePlacement.interactStaticTile(serverPlayer, position)
                ? InteractionResult.CONSUME : InteractionResult.PASS;
    }
}
