package yuki135.blacksouls.mixin.compat;

import yuki135.blacksouls.story.OriginalOpeningService;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="top.theillusivec4.curios.common.network.client.CPacketDestroy",remap=false)
public abstract class OpeningCuriosDestroyMixin {
    @Inject(method="lambda$handle$5(Ljava/util/function/Supplier;)V",at=@At("HEAD"),cancellable=true,remap=false)
    private static void blacksouls$openingDestroy(Supplier<NetworkEvent.Context> supplier,CallbackInfo callback) {
        var player=supplier.get().getSender();
        if (player!=null && player.server.isSameThread() && OriginalOpeningService.inventoryLocked(player)) {
            player.containerMenu.broadcastFullState();
            callback.cancel();
        }
    }
}
