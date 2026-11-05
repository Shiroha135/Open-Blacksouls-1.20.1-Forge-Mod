package com.BlackSouls.BlackSoulsMod.mixin;

import com.BlackSouls.BlackSoulsMod.util.OriginalNpcItems;
import com.BlackSouls.BlackSoulsMod.util.OriginalWeaponForms;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class OriginalWeaponNameMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void blackSouls$showOriginalWeaponLevel(ItemStack stack, CallbackInfoReturnable<Component> callback) {
        if (!stack.hasTag() || !stack.getTag().contains("bs2_upgrade_level", Tag.TAG_INT)
                || stack.getTag().getInt("bs2_upgrade_level") <= 0) return;
        OriginalWeaponForms.Form form = OriginalWeaponForms.get(OriginalNpcItems.sourceId(stack));
        if (form != null && form.upgradeLevel() > 0) {
            callback.setReturnValue(Component.translatable("item.blacksouls.original_weapon.level", callback.getReturnValue(), form.upgradeLevel()));
        }
    }
}
