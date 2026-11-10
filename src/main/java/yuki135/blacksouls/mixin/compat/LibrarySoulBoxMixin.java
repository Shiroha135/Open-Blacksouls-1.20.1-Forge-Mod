package yuki135.blacksouls.mixin.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yuki135.blacksouls.compat.scene.OriginalLibraryProgress;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.block.SoulSynthesisBoxBlock", remap = false)
public abstract class LibrarySoulBoxMixin extends Block {
    protected LibrarySoulBoxMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void blacksouls$defaultVisible(CallbackInfo callback) {
        registerDefaultState(defaultBlockState().setValue(OriginalLibraryProgress.VISIBLE, true));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return OriginalLibraryProgress.hidden(state) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void blacksouls$gateInteraction(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (!OriginalLibraryProgress.canUseBox(level, pos)) callback.setReturnValue(InteractionResult.PASS);
    }
}
