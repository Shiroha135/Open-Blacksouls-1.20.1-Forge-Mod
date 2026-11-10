package yuki135.blacksouls.mixin.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yuki135.blacksouls.compat.scene.OriginalLibraryProgress;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.block.HorizontalModelShapeCompatBlock", remap = false)
public abstract class LibraryBoxShapeMixin {
    @Inject(method = {"getShape", "m_5940_", "getCollisionShape", "m_5939_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void blacksouls$hiddenBoxShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context,
                                         CallbackInfoReturnable<VoxelShape> callback) {
        if (OriginalLibraryProgress.hidden(state)) callback.setReturnValue(Shapes.empty());
    }
}
