package yuki135.blacksouls.mixin;

import yuki135.blacksouls.capability.BSPlayerStats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class OpeningFoodMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void blacksouls$openingReplayFood(Player player,CallbackInfo callback) {
        if (player.getCapability(BSPlayerStats.CAPABILITY).map(stats->stats.openingProfile().previewActive()).orElse(false)) callback.cancel();
    }
}
