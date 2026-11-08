package yuki135.blacksouls.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuki135.blacksouls.client.OriginalOpeningCodaEffects;

@Mixin(ToastComponent.class)
public abstract class OpeningCodaToastMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void blacksouls$hideCodaToasts(GuiGraphics graphics,CallbackInfo callback) {
        if(OriginalOpeningCodaEffects.controlsHud())callback.cancel();
    }
}
