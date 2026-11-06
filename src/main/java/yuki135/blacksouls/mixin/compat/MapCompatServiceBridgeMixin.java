package yuki135.blacksouls.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = {
        "cn.zhenhongliya.blacksouls2compat.integration.BlackSoulsProgressBridge",
        "cn.zhenhongliya.blacksouls2compat.integration.BlackSoulsStatBridge",
        "cn.zhenhongliya.blacksouls2compat.integration.WhiteBearShopBridge"
}, remap = false)
public abstract class MapCompatServiceBridgeMixin {
    @ModifyArg(method = "*", at = @At(value = "INVOKE",
            target = "Ljava/lang/Class;forName(Ljava/lang/String;)Ljava/lang/Class;", remap = false),
            index = 0, remap = false)
    private static String blacksouls$resolveClassName(String className) {
        String prefix = "com.BlackSouls.BlackSoulsMod.";
        return className.startsWith(prefix) ? "yuki135.blacksouls." + className.substring(prefix.length()) : className;
    }
}
