package yuki135.blacksouls.mixin.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuki135.blacksouls.compat.scene.LibrarySoulBoxState;
import yuki135.blacksouls.compat.scene.OriginalLibraryProgress;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.blockentity.SoulSynthesisBoxBlockEntity", remap = false)
public abstract class LibrarySoulBoxEntityMixin implements LibrarySoulBoxState {
    @Shadow(remap = false)
    private boolean introduced;

    @Override
    public boolean blacksouls$introduced() {
        return introduced;
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true, remap = false)
    private void blacksouls$validateActivation(ServerPlayer player, CallbackInfo callback) {
        BlockEntity box = (BlockEntity) (Object) this;
        if (!OriginalLibraryProgress.canUseBox(player.level(), box.getBlockPos())) callback.cancel();
    }

    @Inject(method = "synthesize", at = @At("HEAD"), cancellable = true, remap = false)
    private void blacksouls$validateSynthesis(ServerPlayer player, ResourceLocation soulId, CallbackInfo callback) {
        BlockEntity box = (BlockEntity) (Object) this;
        if (!OriginalLibraryProgress.canUseBox(player.level(), box.getBlockPos())) callback.cancel();
    }

    @Inject(method = "interact", at = @At("TAIL"), remap = false)
    private void blacksouls$rememberOpening(ServerPlayer player, CallbackInfo callback) {
        if (introduced) OriginalLibraryProgress.boxOpened(player.level(), ((BlockEntity) (Object) this).getBlockPos());
    }

    @Inject(method = "resetIntroduction", at = @At("TAIL"), remap = false)
    private void blacksouls$resetOpening(CallbackInfo callback) {
        BlockEntity box = (BlockEntity) (Object) this;
        if (box.getLevel() != null) OriginalLibraryProgress.boxReset(box.getLevel(), box.getBlockPos());
    }
}
