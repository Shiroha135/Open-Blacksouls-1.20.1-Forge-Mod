package com.BlackSouls.BlackSoulsMod.mixin.client;

import com.BlackSouls.BlackSoulsMod.client.font.BSGlobalFont;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(FontManager.class)
public abstract class FontManagerResourceMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private void blacksouls$captureResourceManager(PreparableReloadListener.PreparationBarrier barrier,
                                                    ResourceManager resourceManager,
                                                    ProfilerFiller preparationProfiler,
                                                    ProfilerFiller reloadProfiler,
                                                    Executor preparationExecutor,
                                                    Executor reloadExecutor,
                                                    CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        BSGlobalFont.setResourceManager(resourceManager);
    }
}