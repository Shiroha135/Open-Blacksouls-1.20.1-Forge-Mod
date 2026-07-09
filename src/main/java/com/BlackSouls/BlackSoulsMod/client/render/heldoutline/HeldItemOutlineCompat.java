package com.BlackSouls.BlackSoulsMod.client.render.heldoutline;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class HeldItemOutlineCompat {
    private static Boolean embeddiumLoaded;
    private static Boolean oculusLoaded;
    private static Method irisApiGetInstanceMethod;
    private static Method irisApiIsShaderPackInUseMethod;
    private static boolean irisApiLookupFailed;

    private HeldItemOutlineCompat() {
    }

    public static boolean isEmbeddiumLoaded() {
        if (embeddiumLoaded == null) {
            embeddiumLoaded = ModList.get().isLoaded("embeddium");
        }
        return embeddiumLoaded;
    }

    public static boolean isOculusLoaded() {
        if (oculusLoaded == null) {
            oculusLoaded = ModList.get().isLoaded("oculus");
        }
        return oculusLoaded;
    }

    public static boolean shouldUseEmbeddiumOculusPipeline(Minecraft minecraft) {
        return isOculusShaderPackActive();
    }

    public static boolean isOculusShaderPackActive() {
        if (!isOculusLoaded() || irisApiLookupFailed) {
            return false;
        }

        try {
            if (irisApiGetInstanceMethod == null || irisApiIsShaderPackInUseMethod == null) {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApiGetInstanceMethod = irisApiClass.getMethod("getInstance");
                irisApiIsShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");
            }
            Object irisApi = irisApiGetInstanceMethod.invoke(null);
            Object result = irisApiIsShaderPackInUseMethod.invoke(irisApi);
            return result instanceof Boolean active && active;
        } catch (ReflectiveOperationException | LinkageError exception) {
            irisApiLookupFailed = true;
            return false;
        }
    }

    public static boolean isOculusShadowPass() {
        return false;
    }
}
