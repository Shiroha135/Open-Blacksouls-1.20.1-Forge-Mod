package yuki135.blacksouls.mixin;

import yuki135.blacksouls.story.OriginalOpeningService;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class OpeningInteractionMixin {
    @Shadow public ServerPlayer player;

    @Inject(method="handleContainerClick",at=@At("HEAD"),cancellable=true)
    private void blacksouls$openingInventory(ServerboundContainerClickPacket packet,CallbackInfo callback) {
        if (player.server.isSameThread() && OriginalOpeningService.managed(player) && (packet.getClickType()==net.minecraft.world.inventory.ClickType.THROW || packet.getSlotNum()==-999)) reject(callback);
        else block(callback);
    }

    @Inject(method="handleContainerButtonClick",at=@At("HEAD"),cancellable=true)
    private void blacksouls$openingInventoryButton(ServerboundContainerButtonClickPacket packet,CallbackInfo callback) { block(callback); }

    @Inject(method="handleSetCreativeModeSlot",at=@At("HEAD"),cancellable=true)
    private void blacksouls$openingCreativeInventory(ServerboundSetCreativeModeSlotPacket packet,CallbackInfo callback) {
        if (player.server.isSameThread() && OriginalOpeningService.managed(player) && packet.getSlotNum()<0) reject(callback);
        else block(callback);
    }

    @Inject(method="handleSetCarriedItem",at=@At("HEAD"),cancellable=true)
    private void blacksouls$openingHotbar(ServerboundSetCarriedItemPacket packet,CallbackInfo callback) { block(callback); }

    @Inject(method="handlePlayerAction",at=@At("HEAD"),cancellable=true)
    private void blacksouls$openingItemActions(ServerboundPlayerActionPacket packet,CallbackInfo callback) {
        if (player.server.isSameThread() && OriginalOpeningService.managed(player) && (packet.getAction()==ServerboundPlayerActionPacket.Action.DROP_ITEM || packet.getAction()==ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS)) reject(callback);
        else block(callback);
    }

    private void block(CallbackInfo callback) {
        if (!player.server.isSameThread() || !OriginalOpeningService.inventoryLocked(player)) return;
        reject(callback);
    }

    private void reject(CallbackInfo callback) {
        if (!player.server.isSameThread()) return;
        player.containerMenu.broadcastFullState();
        callback.cancel();
    }
}
