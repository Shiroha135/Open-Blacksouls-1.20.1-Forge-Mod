package yuki135.blacksouls.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuki135.blacksouls.client.OriginalOpeningBonfireMask;

@Mixin(ClientPacketListener.class)
public abstract class OpeningBonfirePacketMixin {
    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void blacksouls$maskBonfireBlock(ClientboundBlockUpdatePacket packet, CallbackInfo callback) {
        OriginalOpeningBonfireMask.refresh();
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
    private void blacksouls$maskBonfireSection(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo callback) {
        OriginalOpeningBonfireMask.refresh();
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void blacksouls$maskBonfireChunk(ClientboundLevelChunkWithLightPacket packet, CallbackInfo callback) {
        OriginalOpeningBonfireMask.refresh();
    }
}
