package yuki135.blacksouls.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yuki135.blacksouls.story.OriginalOpeningBonfireFlow;

@Mixin(BlockBehaviour.class)
public abstract class OpeningBonfireCollisionMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void blacksouls$hiddenBonfireCollision(BlockState state, BlockGetter level, BlockPos pos,
                                                   CollisionContext context, CallbackInfoReturnable<VoxelShape> callback) {
        if (state.is(BlockTags.CAMPFIRES) && context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player && OriginalOpeningBonfireFlow.isHidden(player, pos)) {
            callback.setReturnValue(Shapes.empty());
        }
    }
}
