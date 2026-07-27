package com.BlackSouls.BlackSoulsMod.mixin.client;

import com.BlackSouls.BlackSoulsMod.client.font.BSGlobalFont;
import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(FontSet.class)
public abstract class FontSetGlobalMixin {
    @Shadow
    @Final
    private ResourceLocation name;

    @ModifyVariable(method = "reload", at = @At("HEAD"), argsOnly = true)
    private List<GlyphProvider> blacksouls$prependGlobalFont(List<GlyphProvider> providers) {
        if (!Minecraft.DEFAULT_FONT.equals(name)) {
            return providers;
        }
        return BSGlobalFont.prependToDefaultFont(providers);
    }
}