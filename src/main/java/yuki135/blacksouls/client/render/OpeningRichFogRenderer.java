package yuki135.blacksouls.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class OpeningRichFogRenderer {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"textures/environment/opening_rich_fog.png");
    private static ShaderInstance shader;

    private OpeningRichFogRenderer() { }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        shader=null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"opening_rich_fog"),DefaultVertexFormat.POSITION_TEX),value->shader=value);
        } catch(IOException failure) {
            org.slf4j.LoggerFactory.getLogger(OpeningRichFogRenderer.class).warn("Opening rich fog shader unavailable",failure);
        }
    }

    public static void render(GuiGraphics graphics,float partialTick) {
        var minecraft=Minecraft.getInstance();
        if(shader==null||minecraft.level==null||minecraft.player==null)return;
        graphics.flush();
        var previous=RenderSystem.getShader();
        int previousTexture=RenderSystem.getShaderTexture(0);
        float scale=Math.min(graphics.guiWidth()/640F,graphics.guiHeight()/480F);
        shader.safeGetUniform("Time").set((minecraft.level.getGameTime()%100000+partialTick)/20F);
        shader.safeGetUniform("CanvasSize").set(graphics.guiWidth()/scale,graphics.guiHeight()/scale);
        shader.safeGetUniform("CameraOffset").set((float)(minecraft.player.getX()*32%816),(float)(minecraft.player.getZ()*32%624));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(()->shader);
        RenderSystem.setShaderTexture(0,TEXTURE);
        var matrix=graphics.pose().last().pose();
        var buffer=Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix,0,graphics.guiHeight(),0).uv(0,1).endVertex();
        buffer.vertex(matrix,graphics.guiWidth(),graphics.guiHeight(),0).uv(1,1).endVertex();
        buffer.vertex(matrix,graphics.guiWidth(),0,0).uv(1,0).endVertex();
        buffer.vertex(matrix,0,0,0).uv(0,0).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.setShaderTexture(0,previousTexture);
        RenderSystem.setShader(()->previous);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
