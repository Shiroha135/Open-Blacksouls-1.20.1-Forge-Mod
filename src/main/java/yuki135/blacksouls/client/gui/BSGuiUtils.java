package yuki135.blacksouls.client.gui;

import org.jetbrains.annotations.NotNull;

import yuki135.blacksouls.BlackSouls;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class BSGuiUtils {

    public static final ResourceLocation RM_WINDOW_SKIN = new ResourceLocation(BlackSouls.MODID, "textures/gui/window.png");
    private static final ResourceLocation RM_WINDOW_BACKGROUND = new ResourceLocation(BlackSouls.MODID, "textures/gui/window_back.png");
    private static final int TEX_SIZE = 128;        
    private static final int BG_U = 0;              
    private static final int BG_V = 64;             
    private static final int BORDER_U = 64;        
    private static final int BORDER_V = 0;         
    private static final int CORNER_TEX_SIZE = 16;  
    private static final int BG_RENDER_SIZE = 32;   

    public static void drawRMSelection(GuiGraphics graphics,int x,int y,int width,int height) {
        if (width <= 0 || height <= 0) return;
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float pulse = 0.72F + 0.18F * (float) Math.cos(net.minecraft.Util.getMillis() * Math.PI / 1000.0D);
        graphics.setColor(1,1,1,1);
        int middle = y + Math.max(1,height / 2);
        graphics.fillGradient(x,y,x+width,middle,selectionColor(0xADADAD,0.34F*pulse),selectionColor(0x797979,0.48F*pulse));
        graphics.fillGradient(x,middle,x+width,y+height,selectionColor(0x797979,0.48F*pulse),selectionColor(0xC4C4C4,0.32F*pulse));
        if (width > 2 && height > 2) {
            graphics.fill(x+1,y,x+width-1,y+1,selectionColor(0xFFFFFF,0.66F*pulse));
            graphics.fill(x+1,y+height-1,x+width-1,y+height,selectionColor(0xFFFFFF,0.56F*pulse));
            graphics.fill(x,y+1,x+1,y+height-1,selectionColor(0xEFEFEF,0.6F*pulse));
            graphics.fill(x+width-1,y+1,x+width,y+height-1,selectionColor(0xEFEFEF,0.6F*pulse));
        }
        graphics.flush();
        graphics.setColor(1,1,1,1);
        RenderSystem.disableBlend();
    }

    private static int selectionColor(int rgb,float opacity) {
        return Math.round(255 * Math.max(0,Math.min(1,opacity))) << 24 | rgb;
    }

    public static void drawRMSelectionBounds(GuiGraphics graphics,int left,int top,int right,int bottom) {
        drawRMSelection(graphics,left,top,right-left,bottom-top);
    }

    public static void drawRMWindow(@NotNull GuiGraphics guiGraphics, int x, int y, int width, int height) {
        drawRMWindow(guiGraphics, x, y, width, height, 8);
    }
    public static void drawRMWindow(@NotNull GuiGraphics guiGraphics, int x, int y, int width, int height, int borderThickness) {
        drawRMWindow(guiGraphics, x, y, width, height, borderThickness, BG_RENDER_SIZE);
    }
    public static void drawRMWindow(@NotNull GuiGraphics guiGraphics, int x, int y, int width, int height, int borderThickness, int backgroundTileSize) {
        drawRMWindow(guiGraphics, x, y, width, height, borderThickness, backgroundTileSize, 1.0F);
    }
    public static void drawRMWindow(@NotNull GuiGraphics guiGraphics, int x, int y, int width, int height, int borderThickness, int backgroundTileSize, float backgroundOpacity) {
        if (width <= 0 || height <= 0) return;
        borderThickness = Math.max(1, Math.min(borderThickness, Math.min(width, height) / 2));
        backgroundTileSize = Math.max(1, backgroundTileSize);
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int bgOffset = borderThickness / 2;
        int bgStartX = x + bgOffset;
        int bgStartY = y + bgOffset;
        int bgWidth = width - bgOffset * 2;
        int bgHeight = height - bgOffset * 2;
        boolean translucent=backgroundOpacity<1.0F;
        if(!translucent) {
            guiGraphics.fill(bgStartX, bgStartY, bgStartX + bgWidth, bgStartY + bgHeight, 0xFF000000);
            guiGraphics.flush();
        }
        if(translucent)guiGraphics.setColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, backgroundOpacity));
        else guiGraphics.setColor(109.0F / 255.0F, 1.0F / 255.0F, 1.0F / 255.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        boolean composed=translucent&&yuki135.blacksouls.client.render.OriginalWindowBackground.draw(guiGraphics,RM_WINDOW_SKIN,bgStartX,bgStartY,bgWidth,bgHeight,backgroundTileSize,backgroundOpacity);
        
        for (int i = 0; !composed&&i < bgWidth; i += backgroundTileSize) {
            for (int j = 0; j < bgHeight; j += backgroundTileSize) {
                int drawW = Math.min(backgroundTileSize, bgWidth - i);
                int drawH = Math.min(backgroundTileSize, bgHeight - j);
                int texW = Math.max(1, Math.round(drawW * 64.0F / backgroundTileSize));
                int texH = Math.max(1, Math.round(drawH * 64.0F / backgroundTileSize));
                if(translucent)guiGraphics.blit(RM_WINDOW_BACKGROUND, bgStartX + i, bgStartY + j, drawW, drawH, 0, 0, texW, texH, 64, 64);
                else guiGraphics.blit(RM_WINDOW_SKIN, bgStartX + i, bgStartY + j, drawW, drawH, BG_U, BG_V, texW, texH, TEX_SIZE, TEX_SIZE);
            }
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        float scale = (float) CORNER_TEX_SIZE / borderThickness;
        int edgeRenderLen = Math.max(1, (int) (32 / scale));
        int rightX = x + width - borderThickness;
        int bottomY = y + height - borderThickness;
        int edgeU = BORDER_U + 64 - CORNER_TEX_SIZE;
        int edgeV = BORDER_V + 64 - CORNER_TEX_SIZE;
        guiGraphics.blit(RM_WINDOW_SKIN, x, y, borderThickness, borderThickness, BORDER_U, BORDER_V, CORNER_TEX_SIZE, CORNER_TEX_SIZE, TEX_SIZE, TEX_SIZE); 
        guiGraphics.blit(RM_WINDOW_SKIN, rightX, y, borderThickness, borderThickness, edgeU, BORDER_V, CORNER_TEX_SIZE, CORNER_TEX_SIZE, TEX_SIZE, TEX_SIZE); 
        guiGraphics.blit(RM_WINDOW_SKIN, x, bottomY, borderThickness, borderThickness, BORDER_U, edgeV, CORNER_TEX_SIZE, CORNER_TEX_SIZE, TEX_SIZE, TEX_SIZE); 
        guiGraphics.blit(RM_WINDOW_SKIN, rightX, bottomY, borderThickness, borderThickness, edgeU, edgeV, CORNER_TEX_SIZE, CORNER_TEX_SIZE, TEX_SIZE, TEX_SIZE); 
        int innerWidth = width - borderThickness * 2;
        for (int i = 0; i < innerWidth; i += edgeRenderLen) {
            int drawW = Math.min(edgeRenderLen, innerWidth - i);
            int texW = (int) (drawW * scale);
            int currentX = x + borderThickness + i;
            guiGraphics.blit(RM_WINDOW_SKIN, currentX, y, drawW, borderThickness, BORDER_U + CORNER_TEX_SIZE, BORDER_V, texW, CORNER_TEX_SIZE, TEX_SIZE, TEX_SIZE); 
            guiGraphics.blit(RM_WINDOW_SKIN, currentX, bottomY, drawW, borderThickness, BORDER_U + CORNER_TEX_SIZE, edgeV, texW, CORNER_TEX_SIZE, TEX_SIZE, TEX_SIZE); 
        }
        int innerHeight = height - borderThickness * 2;
        for (int i = 0; i < innerHeight; i += edgeRenderLen) {
            int drawH = Math.min(edgeRenderLen, innerHeight - i);
            int texH = (int) (drawH * scale);
            int currentY = y + borderThickness + i;
            guiGraphics.blit(RM_WINDOW_SKIN, x, currentY, borderThickness, drawH, BORDER_U, BORDER_V + CORNER_TEX_SIZE, CORNER_TEX_SIZE, texH, TEX_SIZE, TEX_SIZE); 
            guiGraphics.blit(RM_WINDOW_SKIN, rightX, currentY, borderThickness, drawH, edgeU, BORDER_V + CORNER_TEX_SIZE, CORNER_TEX_SIZE, texH, TEX_SIZE, TEX_SIZE); 
        }
        RenderSystem.disableBlend();
    }
}
