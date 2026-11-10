package yuki135.blacksouls.mixin.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuki135.blacksouls.compat.scene.OriginalLibraryProgress;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.scene.ProgressTorchHandler", remap = false)
public abstract class LibraryProgressTorchMixin {
    @Inject(method = "onLevelTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void blacksouls$authoredLibraryRow(TickEvent.LevelTickEvent event, CallbackInfo callback) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level
                && level.getGameTime() % 20L == 0L && OriginalLibraryProgress.terrainMatches(level)) callback.cancel();
    }
}
