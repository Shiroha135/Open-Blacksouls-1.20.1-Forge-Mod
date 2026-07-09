package com.BlackSouls.BlackSoulsMod.client.render.heldoutline;

import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

public final class HeldItemBloomShaderRegistry {
    @Nullable
    private static ShaderInstance shader;

    private HeldItemBloomShaderRegistry() {
    }

    public static void setShader(@Nullable ShaderInstance shaderInstance) {
        shader = shaderInstance;
    }

    @Nullable
    public static ShaderInstance getShader() {
        return shader;
    }
}
