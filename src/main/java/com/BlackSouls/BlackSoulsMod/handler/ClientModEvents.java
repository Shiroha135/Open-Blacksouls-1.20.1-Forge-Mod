package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiBSConfig;
import com.BlackSouls.BlackSoulsMod.client.render.*;
import com.BlackSouls.BlackSoulsMod.client.tooltip.ClientSpongeNameTooltipComponent;
import com.BlackSouls.BlackSoulsMod.client.tooltip.SpongeNameTooltipComponent;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalTurnBattleEnemy;
import com.shiroha.mmdskin.render.entity.MmdSkinRenderFactory;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.RabbitRenderer;
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
        event.registerEntityRenderer(BSEntityRegistry.RED_HOOD.get(), new MmdSkinRenderFactory<>("小红帽"));
        event.registerEntityRenderer(BSEntityRegistry.RABBIT_HOLE_NPC.get(), RabbitRenderer::new);
        event.registerEntityRenderer(BSEntityRegistry.HAIL_CAESAR.get(), RenderHailCaesar::new);
        event.registerEntityRenderer(BSEntityRegistry.TEST_DUMMY.get(), RenderTestDummy::new);
        event.registerEntityRenderer(BSEntityRegistry.THROWN_BLADE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(BSEntityRegistry.HELL_PRINCE.get(), RenderHellPrince::new);
        event.registerEntityRenderer(BSEntityRegistry.MEAT_WALL.get(), RenderMeatWall::new);
        event.registerEntityRenderer(BSEntityRegistry.CORPSE_EATING_RABBIT.get(), RenderCorpseEatingRabbit::new);
        event.registerEntityRenderer(BSEntityRegistry.ORIGINAL_ENEMY.get(), RenderOriginalDatabaseEnemy::new);
        event.registerEntityRenderer(BSEntityRegistry.HEADLESS_UNDEAD.get(),
                context -> new RenderOriginalTurnBattleEnemy(
                        context, EntityOriginalTurnBattleEnemy.Profile.HEADLESS_UNDEAD));
        event.registerEntityRenderer(BSEntityRegistry.CORRUPT_DOG.get(),
                context -> new RenderOriginalTurnBattleEnemy(
                        context, EntityOriginalTurnBattleEnemy.Profile.CORRUPT_DOG));
        event.registerEntityRenderer(BSEntityRegistry.WEREWOLF.get(),
                context -> new RenderOriginalTurnBattleEnemy(
                        context, EntityOriginalTurnBattleEnemy.Profile.WEREWOLF));
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RenderMeatWall.LAYER_LOCATION, RenderMeatWall.MeatWallModel::createBodyLayer);
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
