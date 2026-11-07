package yuki135.blacksouls.mixin.compat;

import yuki135.blacksouls.story.OriginalOpeningService;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="top.theillusivec4.curios.common.network.client.CPacketOpenCurios",remap=false)
public abstract class OpeningCuriosOpenMixin {
    @Inject(method="lambda$handle$1(Ljava/util/function/Supplier;Ltop/theillusivec4/curios/common/network/client/CPacketOpenCurios;)V",at=@At("HEAD"),cancellable=true,remap=false)
    private static void blacksouls$openingMenu(Supplier<NetworkEvent.Context> supplier,@Coerce Object packet,CallbackInfo callback) {
        var player=supplier.get().getSender();
        if (player!=null && player.server.isSameThread() && OriginalOpeningService.inventoryLocked(player)) {
            player.containerMenu.broadcastFullState();
            callback.cancel();
        }
    }
}
