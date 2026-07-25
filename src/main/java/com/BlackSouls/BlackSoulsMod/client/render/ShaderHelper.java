package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.render.heldoutline.HeldItemBloomBlurShaderRegistry;
import com.BlackSouls.BlackSoulsMod.client.render.heldoutline.HeldItemBloomShaderRegistry;
import com.BlackSouls.BlackSoulsMod.client.render.heldoutline.HeldItemOutlineShaderRegistry;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;

@Mod.EventBusSubscriber(
        modid = BlackSouls.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ShaderHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static ShaderInstance flowBarShader;
    public static ShaderInstance rainbowTextShader;
    public static ShaderInstance spongeNameShader;
    public static ShaderInstance cosmicGuiShader;
    public static ShaderInstance goldOutlineShader;
    public static ShaderInstance heldItemOutlineShader;
    public static ShaderInstance heldItemBloomShader;
    public static ShaderInstance heldItemBloomBlurShader;
    public static ShaderInstance chronoStarShader;
    public static ShaderInstance fadedBannerShader;
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {

        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "flow_bar"),
                            DefaultVertexFormat.POSITION_TEX_COLOR
                    ),
                    shader -> flowBarShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "rainbow_text"),
                            DefaultVertexFormat.POSITION_TEX
                    ),
                    shader -> rainbowTextShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "sponge_name_text"),
                            DefaultVertexFormat.POSITION_TEX
                    ),
                    shader -> spongeNameShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "cosmic_gui"),
                            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
                    ),
                    shader -> cosmicGuiShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "gold_outline"),
                            DefaultVertexFormat.NEW_ENTITY
                    ),
                    shader -> goldOutlineShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "held_item_outline"),
                            DefaultVertexFormat.POSITION_TEX
                    ),
                    shader -> {
                        heldItemOutlineShader = shader;
                        HeldItemOutlineShaderRegistry.setShader(shader);
                    }
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "held_item_bloom"),
                            DefaultVertexFormat.POSITION_TEX
                    ),
                    shader -> {
                        heldItemBloomShader = shader;
                        HeldItemBloomShaderRegistry.setShader(shader);
                    }
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "held_item_bloom_blur"),
                            DefaultVertexFormat.POSITION_TEX
                    ),
                    shader -> {
                        heldItemBloomBlurShader = shader;
                        HeldItemBloomBlurShaderRegistry.setShader(shader);
                    }
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "chrono_star"),
                            DefaultVertexFormat.POSITION_TEX_COLOR
                    ),
                    shader -> chronoStarShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(BlackSouls.MODID, "faded_banner"),
                            DefaultVertexFormat.POSITION_TEX
                    ),
                    shader -> fadedBannerShader = shader
            );

            LOGGER.info("BlackSouls shaders loaded successfully.");

        } catch (IOException e) {
            LOGGER.error("Failed to load shaders!", e);
        }
    }

}
