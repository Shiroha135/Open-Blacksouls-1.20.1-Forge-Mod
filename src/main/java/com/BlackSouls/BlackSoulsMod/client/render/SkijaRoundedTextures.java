package com.BlackSouls.BlackSoulsMod.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused", "deprecation"})
public final class SkijaRoundedTextures {
    private static final int SUPERSAMPLE = 8;
    private static final Map<String, RoundedMask> CACHE = new HashMap<>();
    private static int prewarmedScale = -1;

    private SkijaRoundedTextures() {
    }

    public static RoundedMask fill(float width, float height, float radius) {
        return mask(width, height, radius, 0.0F);
    }

    public static RoundedMask stroke(float width, float height, float radius, float thickness) {
        return mask(width, height, radius, Math.max(0.0F, thickness));
    }

    public static void prewarm() {
        int scale = displayScale();
        if (prewarmedScale == scale) {
            return;
        }
        float[] radii = {2.0F, 2.5F, 4.0F, 7.0F, 8.0F, 9.0F, 10.0F, 18.0F, 24.0F};
        for (float radius : radii) {
            fill(radius * 2.0F + 8.0F, radius * 2.0F + 8.0F, radius);
        }
        stroke(44.0F, 44.0F, 18.0F, 1.25F);
        stroke(56.0F, 56.0F, 24.0F, 1.25F);
        prewarmedScale = scale;
    }

    private static RoundedMask mask(float width, float height, float radius, float thickness) {
        int displayScale = displayScale();
        float clampedRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
        int safeRadius = Math.max(0, Math.round(clampedRadius * displayScale));
        int safeThickness = thickness <= 0.0F
                ? 0
                : Math.max(1, Math.round(thickness * displayScale * SUPERSAMPLE));
        String key = (safeThickness == 0 ? "fill" : "stroke")
                + "_s" + SUPERSAMPLE + "_g" + displayScale + "_r" + safeRadius
                + (safeThickness == 0 ? "" : "_t" + safeThickness);
        return CACHE.computeIfAbsent(
                key,
                ignored -> createRoundedMask(key, displayScale, safeRadius,
                        safeThickness / (float) SUPERSAMPLE)
        );
    }

    private static int displayScale() {
        return Math.max(1, (int) Math.ceil(Minecraft.getInstance().getWindow().getGuiScale()));
    }

    private static RoundedMask createRoundedMask(String key, int displayScale,
                                                 int radius, float strokeWidth) {
        int centerSize = Math.max(2, displayScale * 2);
        int size = Math.max(centerSize, radius * 2 + centerSize);
        ResourceLocation texture = createRoundedTexture(key, size, radius, strokeWidth);
        return new RoundedMask(texture, radius / (float) size, radius / (float) displayScale);
    }

    private static ResourceLocation createRoundedTexture(String key, int size,
                                                         int radius, float strokeWidth) {
        int scaledSize = size * SUPERSAMPLE;
        float scaledRadius = radius * SUPERSAMPLE;
        float scaledStrokeWidth = strokeWidth * SUPERSAMPLE;

        try (Surface highSurface = Surface.makeRasterN32Premul(scaledSize, scaledSize);
             Surface finalSurface = Surface.makeRasterN32Premul(size, size)) {
            Canvas highCanvas = highSurface.getCanvas();
            highCanvas.clear(0x00000000);

            try (Paint paint = new Paint().setAntiAlias(true).setColor(0xFFFFFFFF)) {
                if (scaledStrokeWidth <= 0.0F) {
                    highCanvas.drawRRect(
                            RRect.makeXYWH(0.0F, 0.0F, scaledSize, scaledSize, scaledRadius),
                            paint
                    );
                } else {
                    float halfStroke = scaledStrokeWidth * 0.5F;
                    paint.setMode(PaintMode.STROKE);
                    paint.setStrokeWidth(scaledStrokeWidth);
                    highCanvas.drawRRect(
                            RRect.makeXYWH(
                                    halfStroke,
                                    halfStroke,
                                    Math.max(1.0F, scaledSize - scaledStrokeWidth),
                                    Math.max(1.0F, scaledSize - scaledStrokeWidth),
                                    Math.max(0.0F, scaledRadius - halfStroke)
                            ),
                            paint
                    );
                }
            }

            Canvas finalCanvas = finalSurface.getCanvas();
            finalCanvas.clear(0x00000000);
            try (Image highImage = highSurface.makeImageSnapshot()) {
                finalCanvas.drawImageRect(
                        highImage,
                        Rect.makeWH(scaledSize, scaledSize),
                        Rect.makeWH(size, size),
                        SamplingMode.MITCHELL,
                        null,
                        true
                );
            }

            try (Image image = finalSurface.makeImageSnapshot();
                 Data pngData = image.encodeToData(EncodedImageFormat.PNG)) {
                if (pngData == null) {
                    throw new IllegalStateException("Skija PNG encoding returned null: " + key);
                }
                ByteBuffer pngBytes = pngData.toByteBuffer();
                return upload(key, NativeImage.read(pngBytes));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create Skija rounded texture: " + key, exception);
        }
    }

    private static ResourceLocation upload(String key, NativeImage nativeImage) {
        DynamicTexture texture = new DynamicTexture(nativeImage);
        texture.setFilter(true, false);
        texture.upload();
        return Minecraft.getInstance().getTextureManager().register("blacksouls_skija_round/" + key, texture);
    }

    public record RoundedMask(ResourceLocation texture, float cornerUv, float radius) {
    }
}
