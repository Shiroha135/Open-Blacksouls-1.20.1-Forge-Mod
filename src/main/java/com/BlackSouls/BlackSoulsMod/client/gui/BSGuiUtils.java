package com.BlackSouls.BlackSoulsMod.client.gui;

import org.jetbrains.annotations.NotNull;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class BSGuiUtils {

    public static final ResourceLocation RM_WINDOW_SKIN = new ResourceLocation(BlackSouls.MODID, "textures/gui/window.png");
    private static final int TEX_SIZE = 128;        
    private static final int BG_U = 0;              
    private static final int BG_V = 64;             
    private static final int BORDER_U = 64;        
    private static final int BORDER_V = 0;         
    private static final int CORNER_TEX_SIZE = 16;  
    private static final int BG_RENDER_SIZE = 32;   

    public static void drawRMWindow(@NotNull GuiGraphics guiGraphics, int x, int y, int width, int height) {
        drawRMWindow(guiGraphics, x, y, width, height, 8);
    }
    public static void drawRMWindow(@NotNull GuiGraphics guiGraphics, int x, int y, int width, int height, int borderThickness) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int bgOffset = borderThickness / 2;
        int bgStartX = x + bgOffset;
        int bgStartY = y + bgOffset;
        int bgWidth = width - bgOffset * 2;
        int bgHeight = height - bgOffset * 2;
        guiGraphics.fill(bgStartX, bgStartY, bgStartX + bgWidth, bgStartY + bgHeight, 0xFF000000);
        guiGraphics.setColor(109.0F / 255.0F, 1.0F / 255.0F, 1.0F / 255.0F, 1.0F);
        
        for (int i = 0; i < bgWidth; i += BG_RENDER_SIZE) {
            for (int j = 0; j < bgHeight; j += BG_RENDER_SIZE) {
                int drawW = Math.min(BG_RENDER_SIZE, bgWidth - i);
                int drawH = Math.min(BG_RENDER_SIZE, bgHeight - j);
                int texW = drawW * 2;
                int texH = drawH * 2;
                guiGraphics.blit(RM_WINDOW_SKIN, bgStartX + i, bgStartY + j, drawW, drawH, BG_U, BG_V, texW, texH, TEX_SIZE, TEX_SIZE);
            }
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        
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