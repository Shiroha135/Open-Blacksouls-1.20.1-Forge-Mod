package yuki135.blacksouls.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuki135.blacksouls.story.OriginalOpeningBonfireFlow;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {
    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void blacksouls$hiddenOpeningShape(BlockState state, BlockGetter level, BlockPos pos,
                                               CollisionContext context, CallbackInfoReturnable<VoxelShape> callback) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player
                && OriginalOpeningBonfireFlow.isHidden(player, pos)) callback.setReturnValue(Shapes.empty());
    }

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void blacksouls$hiddenOpeningFire(BlockState state, Level level, BlockPos pos,
                                              Entity entity, CallbackInfo callback) {
        if (entity instanceof Player player && OriginalOpeningBonfireFlow.isHidden(player, pos)) callback.cancel();
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void blacksouls$startUnlit(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> callback
    ) {
        BlockState state = callback.getReturnValue();
        if (state != null && state.hasProperty(CampfireBlock.LIT)) {
            callback.setReturnValue(state.setValue(CampfireBlock.LIT, false));
        }
    }
}
