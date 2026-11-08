package yuki135.blacksouls.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class OriginalWindowBackground {
    private static ShaderInstance shader;

    private OriginalWindowBackground() { }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        shader=null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"original_window_background"),DefaultVertexFormat.POSITION_TEX),value->shader=value);
        } catch(IOException failure) {
            org.slf4j.LoggerFactory.getLogger(OriginalWindowBackground.class).warn("Original window background shader unavailable",failure);
        }
    }

    public static boolean draw(GuiGraphics graphics,ResourceLocation texture,int x,int y,int width,int height,int tileSize,float opacity) {
        if(shader==null)return false;
        graphics.flush();
        var previous=RenderSystem.getShader();int previousTexture=RenderSystem.getShaderTexture(0);
        shader.safeGetUniform("WindowSize").set((float)width,(float)height);
        shader.safeGetUniform("TileSize").set((float)tileSize);
        shader.safeGetUniform("BackOpacity").set(opacity);
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,texture);
        var matrix=graphics.pose().last().pose();var buffer=Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix,x,y+height,0).uv(0,1).endVertex();buffer.vertex(matrix,x+width,y+height,0).uv(1,1).endVertex();
        buffer.vertex(matrix,x+width,y,0).uv(1,0).endVertex();buffer.vertex(matrix,x,y,0).uv(0,0).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.setShaderTexture(0,previousTexture);RenderSystem.setShader(()->previous);
        return true;
    }
}
