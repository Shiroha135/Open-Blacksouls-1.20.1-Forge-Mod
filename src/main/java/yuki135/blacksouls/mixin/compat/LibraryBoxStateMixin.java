package yuki135.blacksouls.mixin.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuki135.blacksouls.compat.scene.OriginalLibraryProgress;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.block.HorizontalCompatBlock", remap = false)
public abstract class LibraryBoxStateMixin {
    @Inject(method = {"createBlockStateDefinition", "m_7926_"}, at = @At("TAIL"), remap = false)
    private void blacksouls$libraryBoxProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo callback) {
        if (getClass().getName().equals("cn.zhenhongliya.blacksouls2compat.block.SoulSynthesisBoxBlock")) {
            builder.add(OriginalLibraryProgress.VISIBLE);
        }
    }
}
