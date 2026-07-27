package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.heldoutline.HeldItemOutlineCompat;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientStatusRenderer {

    private static final double ENTITY_STATUS_Y_OFFSET = 0.82D;

    @SubscribeEvent
    public static void onRenderEntityStatus(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || entity instanceof Player || entity instanceof ArmorStand || entity.distanceToSqr(mc.player) > 400 || !hasClearStatusLine(mc, entity)) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        Font font = mc.font;

        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }

        poseStack.pushPose();

        poseStack.translate(0.0D, entity.getBbHeight() + ENTITY_STATUS_Y_OFFSET, 0.0D);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.015F, -0.015F, 0.015F);

        Matrix4f matrix = poseStack.last().pose();

        float hp = entity.getHealth();
        float maxHp = entity.getMaxHealth();
        float hpPercent = Math.max(0.0f, Math.min(1.0f, maxHp > 0 ? hp / maxHp : 0));

        float w = 80f;
        float h = 12f; 
        float x = -w / 2f;
        float y = 0f;

        drawActiveEffects(entity, matrix, mc);

        drawPixelBorderAndBg(matrix, x, y, w, h);

        long currentTime = Util.getMillis();
        boolean oculusShaderPackActive = HeldItemOutlineCompat.isOculusShaderPackActive();
        if (ShaderHelper.flowBarShader != null && !oculusShaderPackActive) {
            float smoothTime = (currentTime % 1000000L) / 1000.0f;
            ShaderInstance shader = ShaderHelper.flowBarShader;
            AbstractUniform gameTime = shader.safeGetUniform("GameTime");
            if (gameTime != null) {
                gameTime.set(smoothTime);
            }
            RenderSystem.setShader(() -> ShaderHelper.flowBarShader);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder builder = tesselator.getBuilder();

            float fillWidth = w * hpPercent;
            drawShaderQuad(builder, matrix, x, y, fillWidth, h, 1.0f, 0.1f, 0.1f, 1.0f);

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
        } else if (oculusShaderPackActive) {
            drawOculusFlowBar(matrix, x, y, w * hpPercent, h, currentTime);
        } else {
            fillSolid(matrix, x, y, x + (w * hpPercent), y + h, 0xFFDD2222);
        }

        poseStack.translate(0.0D, 0.0D, -0.01D);
        Matrix4f textMatrix = poseStack.last().pose();

        String hpText = formatHealthValue(hp) + " / " + formatHealthValue(maxHp);
        float textX = x + w - font.width(hpText) - 4f;
        float textY = y + 2f;

        font.drawInBatch(hpText, textX, textY, 0xFFFFFF, false, textMatrix, buffer, Font.DisplayMode.NORMAL, 0, event.getPackedLight());

        poseStack.popPose();

        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
    }

    private static void drawActiveEffects(LivingEntity entity, Matrix4f matrix, Minecraft mc) {
        List<MobEffectInstance> activeEffects = entity.getActiveEffects().stream()
                .filter(effect -> {
                    net.minecraft.resources.ResourceLocation id =
                            ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                    return id != null && BlackSouls.MODID.equals(id.getNamespace());
                })
                .toList();
        int totalIcons = activeEffects.size();
        if (totalIcons == 0) return;

        float iconSize = 10f;
        float gap = 2f;
        int maxIconsPerRow = 6;
        int index = 0;

        for (MobEffectInstance instance : activeEffects) {
            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(instance.getEffect());
            if (sprite != null) {
                int row = index / maxIconsPerRow;
                int col = index % maxIconsPerRow;

                int iconsInThisRow = Math.min(maxIconsPerRow, totalIcons - row * maxIconsPerRow);
                float rowWidth = iconsInThisRow * iconSize + (iconsInThisRow - 1) * gap;

                float startX = -rowWidth / 2f;
                float currentX = startX + col * (iconSize + gap);
                float currentY = -iconSize - 2f - (row * (iconSize + gap));
                fillSolid(matrix, currentX, currentY, currentX + iconSize, currentY + iconSize, 0x88000000);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, sprite.atlasLocation());

                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder buffer = tesselator.getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

                float u0 = sprite.getU0();
                float u1 = sprite.getU1();
                float v0 = sprite.getV0();
                float v1 = sprite.getV1();

                buffer.vertex(matrix, currentX, currentY + iconSize, 0).uv(u0, v1).endVertex();
                buffer.vertex(matrix, currentX + iconSize, currentY + iconSize, 0).uv(u1, v1).endVertex();
                buffer.vertex(matrix, currentX + iconSize, currentY, 0).uv(u1, v0).endVertex();
                buffer.vertex(matrix, currentX, currentY, 0).uv(u0, v0).endVertex();

                tesselator.end();
            }
            index++;
        }
        RenderSystem.disableBlend();
    }

    private static void drawPixelBorderAndBg(Matrix4f matrix, float x, float y, float w, float h) {
        fillSolid(matrix, x, y, x + w, y + h, 0xAA000000);
        int borderColor = 0xFF1A1A1A;
        fillSolid(matrix, x, y - 1, x + w, y, borderColor);
        fillSolid(matrix, x, y + h, x + w, y + h + 1, borderColor);
        fillSolid(matrix, x - 1, y, x, y + h, borderColor);
        fillSolid(matrix, x + w, y, x + w + 1, y + h, borderColor);
    }

    private static void fillSolid(Matrix4f matrix, float minX, float minY, float maxX, float maxY, int color) {
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, minX, maxY, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, maxY, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, minY, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, minY, 0.0F).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawShaderQuad(BufferBuilder buffer, Matrix4f matrix, float x, float y, float width, float height, float r, float g, float b, float a) {
        if (width <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(matrix, x,         y + height, 0).uv(0.0f, 1.0f).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).uv(1.0f, 1.0f).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x + width, y,          0).uv(1.0f, 0.0f).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x,         y,          0).uv(0.0f, 0.0f).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawOculusFlowBar(Matrix4f matrix, float x, float y, float width, float height, long time) {
        if (width <= 0) return;

        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        drawFlatQuad(matrix, x, y, x + width, y + height, 0.42F, 0.02F, 0.04F, 0.50F);

        float phase = (time % 1150L) / 1150.0F;
        float sweepWidth = Math.min(width, 18.0F);
        float sweepX = x + (width + sweepWidth) * phase - sweepWidth;
        float sx0 = Math.max(x, sweepX);
        float sx1 = Math.min(x + width, sweepX + sweepWidth);
        if (sx1 > sx0) {
            drawFlatQuad(matrix, sx0, y, sx1, y + height, 0.92F, 0.18F, 0.20F, 0.58F);
        }

        drawFlatQuad(matrix, x, y, x + width, y + 1.0F, 1.0F, 0.45F, 0.42F, 0.34F);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawFlatQuad(Matrix4f matrix, float minX, float minY, float maxX, float maxY,
                                     float red, float green, float blue, float alpha) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, minX, maxY, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, maxX, maxY, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, maxX, minY, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, minX, minY, 0.0F).color(red, green, blue, alpha).endVertex();
        Tesselator.getInstance().end();
    }

    private static boolean hasClearStatusLine(Minecraft mc, LivingEntity entity) {
        if (mc.player == null) return false;
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        AABB box = entity.getBoundingBox();
        Vec3 targetPos = new Vec3((box.minX + box.maxX) * 0.5D, entity.getEyeY(), (box.minZ + box.maxZ) * 0.5D);
        return hasClearLineTo(mc, entity, cameraPos, targetPos);
    }

    private static boolean hasClearLineTo(Minecraft mc, LivingEntity entity, Vec3 cameraPos, Vec3 targetPos) {
        HitResult hit = entity.level().clip(new ClipContext(cameraPos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(cameraPos) + 0.20D >= targetPos.distanceToSqr(cameraPos);
    }

    private static String formatHealthValue(float value) {
        double displayValue = Math.ceil(value);
        if (displayValue >= 1_000_000_000D) {
            return formatCompact(displayValue / 1_000_000_000D, "B");
        }
        if (displayValue >= 1_000_000D) {
            return formatCompact(displayValue / 1_000_000D, "M");
        }
        if (displayValue >= 1_000D) {
            return formatCompact(displayValue / 1_000D, "K");
        }
        return String.valueOf((int) displayValue);
    }

    private static String formatCompact(double value, String suffix) {
        double rounded = Math.round(value * 10.0D) / 10.0D;
        if (rounded == Math.rint(rounded)) {
            return (long) rounded + suffix;
        }
        return rounded + suffix;
    }
}
