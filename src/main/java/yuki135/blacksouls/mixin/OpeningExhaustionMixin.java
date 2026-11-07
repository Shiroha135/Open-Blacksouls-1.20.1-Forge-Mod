package yuki135.blacksouls.mixin;

import yuki135.blacksouls.capability.BSPlayerStats;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class OpeningExhaustionMixin {
    @Inject(method="causeFoodExhaustion",at=@At("HEAD"),cancellable=true)
    private void blacksouls$openingReplayExhaustion(float amount,CallbackInfo callback) {
        var player=(Player)(Object)this;
        if (player.getCapability(BSPlayerStats.CAPABILITY).map(stats->stats.openingProfile().previewActive()).orElse(false)) callback.cancel();
    }
}
