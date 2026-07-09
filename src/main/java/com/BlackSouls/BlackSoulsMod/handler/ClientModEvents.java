package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiBSConfig;
import com.BlackSouls.BlackSoulsMod.client.render.*;
import com.BlackSouls.BlackSoulsMod.client.tooltip.ClientSpongeNameTooltipComponent;
import com.BlackSouls.BlackSoulsMod.client.tooltip.SpongeNameTooltipComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new GuiBSConfig(parent)));
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BSEntityRegistry.NODEN.get(), RenderNoden::new);
        event.registerEntityRenderer(BSEntityRegistry.HAIL_CAESAR.get(), RenderHailCaesar::new);
        event.registerEntityRenderer(BSEntityRegistry.TEST_DUMMY.get(), RenderTestDummy::new);
        event.registerEntityRenderer(BSEntityRegistry.THROWN_BLADE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(BSEntityRegistry.HELL_PRINCE.get(), RenderHellPrince::new);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleTypes.DAMAGE_INDICATOR, NoOpParticleProvider::new);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) resourceManager ->
                AnimationRegistry.loadAnimations());
    }

    @SubscribeEvent
    public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(SpongeNameTooltipComponent.class, ClientSpongeNameTooltipComponent::new);
    }

    public static class NoOpParticleProvider implements ParticleProvider<SimpleParticleType> {
        public NoOpParticleProvider(SpriteSet spriteSet) {}

        @Nullable
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new Particle(level, x, y, z) {
                @Override
                public void render(com.mojang.blaze3d.vertex.@NotNull VertexConsumer buffer, net.minecraft.client.@NotNull Camera camera, float partialTicks) {}

                @Override
                public @NotNull ParticleRenderType getRenderType() {
                    return ParticleRenderType.NO_RENDER;
                }
            };
        }
    }
}
