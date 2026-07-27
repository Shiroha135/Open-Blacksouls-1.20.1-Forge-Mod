package com.BlackSouls.BlackSoulsMod.client.font;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.List;

public final class BSGlobalFont {
    private static final ResourceLocation FONT_FILE = new ResourceLocation(BlackSouls.MODID, "lolita.ttf");
    private static volatile ResourceManager resourceManager;

    private BSGlobalFont() {
    }

    public static void setResourceManager(ResourceManager manager) {
        resourceManager = manager;
    }

    public static List<GlyphProvider> prependToDefaultFont(List<GlyphProvider> vanillaProviders) {
        ResourceManager manager = resourceManager;
        if (manager == null) {
            return vanillaProviders;
        }
        try {
            TrueTypeGlyphProviderDefinition definition = new TrueTypeGlyphProviderDefinition(
                    FONT_FILE,
                    11.0F,
                    4.0F,
                    new TrueTypeGlyphProviderDefinition.Shift(0.0F, 0.5F),
                    ""
            );
            GlyphProviderDefinition.Loader loader = definition.unpack().left().orElseThrow();
            GlyphProvider provider = loader.load(manager);
            List<GlyphProvider> providers = new ArrayList<>(vanillaProviders.size() + 1);
            providers.add(provider);
            providers.addAll(vanillaProviders);
            BlackSouls.LOGGER.info("Loaded BLACK SOULS global Lolita font.");
            return providers;
        } catch (Exception exception) {
            BlackSouls.LOGGER.error("Failed to load BLACK SOULS global Lolita font.", exception);
            return vanillaProviders;
        }
    }
}