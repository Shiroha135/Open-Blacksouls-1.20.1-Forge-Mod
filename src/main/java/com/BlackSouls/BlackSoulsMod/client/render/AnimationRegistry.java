package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnimationRegistry {
    public static final Map<Integer, VFXAnimation> ANIMATIONS = new HashMap<>();

    private static class RawAnim {
        int id; String name;
        String texture1; int hue1;
        String texture2; int hue2;
        List<List<RawCell>> frames;
    }

    private static class RawCell {
        int pattern; float x; float y; float zoom; float angle; float opacity; boolean mirror;
    }

    private static String processAndGetTexture(String texName, int hue, Map<String, Integer> rowCache, Set<ResourceLocation> generatedCache) {
        if (texName == null || texName.isEmpty()) return "";
        texName = texName.toLowerCase();

        ResourceLocation originalLoc = new ResourceLocation("blacksouls", "textures/vfx/" + texName + ".png");

        if (!rowCache.containsKey(texName)) {
            try {
                var opt = Minecraft.getInstance().getResourceManager().getResource(originalLoc);
                if (opt.isPresent()) {
                    try (InputStream is = opt.get().open()) {
                        BufferedImage img = ImageIO.read(is);
                        if (img != null) rowCache.put(texName, img.getHeight() / 192);
                    }
                }
            } catch (Exception ignored) {}
            rowCache.putIfAbsent(texName, 5);
        }

        if (hue == 0) return originalLoc.toString();

        String dynamicName = texName + "_hue_" + hue;
        ResourceLocation dynamicLoc = new ResourceLocation("blacksouls", "dynamic_vfx/" + dynamicName);
        if (generatedCache.contains(dynamicLoc)) {
            return dynamicLoc.toString();
        }

        try {
            var opt = Minecraft.getInstance().getResourceManager().getResource(originalLoc);
            if (opt.isPresent()) {
                try (InputStream is = opt.get().open()) {
                    BufferedImage bimg = ImageIO.read(is);
                    if (bimg == null) return originalLoc.toString();

                    int width = bimg.getWidth();
                    int height = bimg.getHeight();
                    BufferedImage newBimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    float[] hsb = new float[3];

                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            int argb = bimg.getRGB(x, y);
                            int a = (argb >> 24) & 0xFF;

                            if (a > 0) {
                                int r = (argb >> 16) & 0xFF;
                                int g = (argb >> 8) & 0xFF;
                                int b = argb & 0xFF;

                                Color.RGBtoHSB(r, g, b, hsb);
                                hsb[0] += hue / 360.0f;
                                if (hsb[0] > 1.0f) hsb[0] -= 1.0f;

                                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                                int newArgb = (a << 24) | (rgb & 0xFFFFFF);
                                newBimg.setRGB(x, y, newArgb);
                            } else {
                                newBimg.setRGB(x, y, 0x00000000);
                            }
                        }
                    }

                    NativeImage image = new NativeImage(width, height, false);
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            int argb = newBimg.getRGB(x, y);
                            int a = (argb >> 24) & 0xFF;
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >> 8) & 0xFF;
                            int b = argb & 0xFF;
                            int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                            image.setPixelRGBA(x, y, abgr);
                        }
                    }

                    RenderSystem.recordRenderCall(() -> {
                        Minecraft.getInstance().getTextureManager().register(dynamicLoc, new DynamicTexture(image));
                    });

                    generatedCache.add(dynamicLoc);
                    return dynamicLoc.toString();
                }
            }
        } catch (Exception e) {
            BlackSouls.LOGGER.error("Failed to generate VFX texture {} with hue {}", texName, hue, e);
        }

        return originalLoc.toString();
    }

    public static void loadAnimations() {
        ANIMATIONS.clear();

        try {
            ResourceLocation jsonPath = new ResourceLocation("blacksouls", "bs_animations.json");

            List<RawAnim> rawAnims;
            try (Reader reader = new InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(jsonPath).open(), StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<RawAnim>>(){}.getType();
                rawAnims = new Gson().fromJson(reader, listType);
            }

            Map<String, Integer> textureRowsCache = new HashMap<>();
            Set<ResourceLocation> generatedTextures = new HashSet<>();

            for (RawAnim raw : rawAnims) {
                if (raw == null || raw.frames == null) continue;

                String finalTex1 = processAndGetTexture(raw.texture1, raw.hue1, textureRowsCache, generatedTextures);
                String finalTex2 = processAndGetTexture(raw.texture2, raw.hue2, textureRowsCache, generatedTextures);

                int rows1 = textureRowsCache.getOrDefault(raw.texture1 != null ? raw.texture1.toLowerCase() : "", 5);
                int rows2 = textureRowsCache.getOrDefault(raw.texture2 != null ? raw.texture2.toLowerCase() : "", 5);

                VFXAnimation vfxAnim = new VFXAnimation(finalTex1, finalTex2, rows1, rows2);

                for (List<RawCell> rawFrame : raw.frames) {
                    VFXFrame vfxFrame = new VFXFrame();
                    for (RawCell rawCell : rawFrame) {
                        if (rawCell.pattern < 0) continue;
                        vfxFrame.addCell(new VFXCell(
                                rawCell.pattern,
                                rawCell.x / 32.0f,
                                rawCell.y / 32.0f,
                                (rawCell.zoom / 100.0f) * 0.5f,
                                (rawCell.zoom / 100.0f) * 0.5f,
                                rawCell.angle,
                                rawCell.opacity / 255.0f,
                                rawCell.mirror
                        ));
                    }
                    vfxAnim.frames.add(vfxFrame);
                }
                ANIMATIONS.put(raw.id, vfxAnim);
            }
        } catch (Exception e) {
            BlackSouls.LOGGER.error("Failed to load VFX animations", e);
        }
    }
}
