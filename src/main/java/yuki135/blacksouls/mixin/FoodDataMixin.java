package yuki135.blacksouls.mixin;

import yuki135.blacksouls.util.VanillaHealthScaling;
import yuki135.blacksouls.util.HokoniwaDestination;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V")
    )
    private void blacksouls$scaleNaturalRegeneration(Player player, float amount) {
        if (!HokoniwaDestination.isHokoniwa(player.level().dimension())) {
            player.heal(VanillaHealthScaling.scaleVanillaHealing(player, amount));
        }
    }
}
