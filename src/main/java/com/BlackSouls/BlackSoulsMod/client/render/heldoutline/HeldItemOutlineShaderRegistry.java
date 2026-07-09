package com.BlackSouls.BlackSoulsMod.client.render.heldoutline;

import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

public final class HeldItemOutlineShaderRegistry {
    @Nullable
    private static ShaderInstance shader;

    private HeldItemOutlineShaderRegistry() {
    }

    public static void setShader(@Nullable ShaderInstance shaderInstance) {
        shader = shaderInstance;
    }

    @Nullable
    public static ShaderInstance getShader() {
        return shader;
    }

    @Nullable
    public static ShaderInstance getLoadedShader() {
        return shader;
    }
}
