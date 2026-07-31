package com.shiroha.mmdskin.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import java.util.function.Function;

public class KeyMappingUtil {
    private static Function<KeyMapping, InputConstants.Key> boundKeyGetter = (k) -> InputConstants.UNKNOWN;

    public static void setBoundKeyGetter(Function<KeyMapping, InputConstants.Key> getter) {
        boundKeyGetter = getter;
    }

    public static InputConstants.Key getBoundKey(KeyMapping keyMapping) {
        if (keyMapping == null) return InputConstants.UNKNOWN;
        return boundKeyGetter.apply(keyMapping);
    }
}
