package com.BlackSouls.BlackSoulsMod.mixin.client;

import com.BlackSouls.BlackSoulsMod.client.render.FogGateEffectRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.ShaderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.client.FogGateRenderer", remap = false)
public abstract class FogGateRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void blacksouls$renderShaderFogGate(
            @Coerce Object blockEntityObject,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo callback
    ) {
        if (ShaderHelper.fogGateShader == null || !(blockEntityObject instanceof BlockEntity blockEntity)) {
            return;
        }
        FogGateEffectRenderer.render(blockEntity, partialTick, poseStack, bufferSource);
        callback.cancel();
    }
}
