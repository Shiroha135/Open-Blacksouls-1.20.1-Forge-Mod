package com.BlackSouls.BlackSoulsMod.client.render.heldoutline;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.mixin.client.ItemInHandRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class HeldItemOutlineOculusFallbackEvents {
    private HeldItemOutlineOculusFallbackEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!HeldItemOutlineCompat.isOculusLoaded() || HeldItemOutlineRenderer.isCaptureActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !HeldItemOutlineRenderer.shouldRenderOutlinePass(minecraft)) {
            return;
        }

        HeldItemOutlineRenderer.HandEffectTarget target = HeldItemOutlineRenderer.getRenderableHand(player, event.getHand());
        if (target == null) {
            return;
        }

        event.setCanceled(true);
        ItemStack stack = event.getItemStack();
        PoseStack poseStack = event.getPoseStack();
        ItemInHandRendererAccessor renderer = (ItemInHandRendererAccessor) minecraft.gameRenderer.itemInHandRenderer;
        renderer.blacksouls$renderArmWithItem(player, event.getPartialTick(), event.getInterpolatedPitch(),
                event.getHand(), event.getSwingProgress(), stack, event.getEquipProgress(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight());

        HeldItemOutlineRenderer.beginItemInHandRender(null);
        try {
            MultiBufferSource.BufferSource captureBufferSource = HeldItemOutlineRenderer.getEmbeddiumCaptureBufferSource();
            Matrix4f modelViewMatrix = new Matrix4f(poseStack.last().pose());
            if (!HeldItemOutlineRenderer.beginCapture(minecraft, minecraft.getMainRenderTarget(), event.getHand(),
                    event.getHand(), modelViewMatrix, target.profile(), target.sampledColors())) {
                return;
            }

            try {
                renderer.blacksouls$renderArmWithItem(player, event.getPartialTick(), event.getInterpolatedPitch(),
                        event.getHand(), event.getSwingProgress(), stack, event.getEquipProgress(),
                        poseStack, captureBufferSource, event.getPackedLight());
                captureBufferSource.endBatch();
            } finally {
                HeldItemOutlineRenderer.endCapture();
            }
            HeldItemOutlineRenderer.composite(minecraft, minecraft.getMainRenderTarget(), event.getHand());
        } finally {
            HeldItemOutlineRenderer.endItemInHandRender();
        }
    }
}
