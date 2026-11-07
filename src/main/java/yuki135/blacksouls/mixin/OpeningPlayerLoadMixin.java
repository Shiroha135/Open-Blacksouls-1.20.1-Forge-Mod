package yuki135.blacksouls.mixin;

import yuki135.blacksouls.story.OriginalOpeningEnrollment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class OpeningPlayerLoadMixin {
    @Shadow @Final private PlayerDataStorage playerIo;

    @Inject(method = "load(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("HEAD"), require = 1, allow = 1)
    private void blacksouls$openingLoadHead(ServerPlayer player, CallbackInfoReturnable<CompoundTag> callback) {
        OriginalOpeningEnrollment.beginLoad((PlayerList) (Object) this, playerIo, player);
    }

    @Inject(method = "load(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"), require = 1, allow = 1)
    private void blacksouls$openingLoadReturn(ServerPlayer player, CallbackInfoReturnable<CompoundTag> callback) {
        OriginalOpeningEnrollment.finishLoad((PlayerList) (Object) this, playerIo, player, callback.getReturnValue());
        yuki135.blacksouls.story.OriginalLegacyNameCompletion.loaded(player, callback.getReturnValue());
    }
}
