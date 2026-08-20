package com.BlackSouls.BlackSoulsMod.client.font;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("removal")
public final class BSGlobalFont {
    private static final ResourceLocation FONT_FILE = new ResourceLocation(BlackSouls.MODID, "lolita.ttf");
    private static final String OPENING_PUNCTUATION = "（〔［｛〈《「『【〖〘〚";
    private static final String CLOSING_PUNCTUATION = "）〕］｝〉》」』】〗〙〛";
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
            GlyphProvider provider = new EastAsianPunctuationProvider(loader.load(manager));
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

    private static final class EastAsianPunctuationProvider implements GlyphProvider {
        private final GlyphProvider delegate;

        private EastAsianPunctuationProvider(GlyphProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public GlyphInfo getGlyph(int codePoint) {
            GlyphInfo glyph = delegate.getGlyph(codePoint);
            if (glyph == null) {
                return null;
            }
            boolean opening = OPENING_PUNCTUATION.indexOf(codePoint) >= 0;
            if (!opening && CLOSING_PUNCTUATION.indexOf(codePoint) < 0) {
                return glyph;
            }
            return new AdjustedPunctuationGlyph(glyph, opening);
        }

        @Override
        public IntSet getSupportedGlyphs() {
            return delegate.getSupportedGlyphs();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    private static final class AdjustedPunctuationGlyph implements GlyphInfo {
        private final GlyphInfo delegate;
        private final boolean opening;

        private AdjustedPunctuationGlyph(GlyphInfo delegate, boolean opening) {
            this.delegate = delegate;
            this.opening = opening;
        }

        @Override
        public float getAdvance() {
            return delegate.getAdvance() * 0.5F;
        }

        @Override
        public float getBoldOffset() {
            return delegate.getBoldOffset();
        }

        @Override
        public float getShadowOffset() {
            return delegate.getShadowOffset();
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> stitcher) {
            float shiftX = opening ? -getAdvance() : 0.0F;
            return delegate.bake(glyph -> stitcher.apply(new ShiftedSheetGlyph(glyph, shiftX)));
        }
    }

    private static final class ShiftedSheetGlyph implements SheetGlyphInfo {
        private final SheetGlyphInfo delegate;
        private final float shiftX;

        private ShiftedSheetGlyph(SheetGlyphInfo delegate, float shiftX) {
            this.delegate = delegate;
            this.shiftX = shiftX;
        }

        @Override
        public int getPixelWidth() {
            return delegate.getPixelWidth();
        }

        @Override
        public int getPixelHeight() {
            return delegate.getPixelHeight();
        }

        @Override
        public void upload(int x, int y) {
            delegate.upload(x, y);
        }

        @Override
        public boolean isColored() {
            return delegate.isColored();
        }

        @Override
        public float getOversample() {
            return delegate.getOversample();
        }

        @Override
        public float getBearingX() {
            return delegate.getBearingX() + shiftX;
        }

        @Override
        public float getBearingY() {
            return delegate.getBearingY();
        }
    }
}
