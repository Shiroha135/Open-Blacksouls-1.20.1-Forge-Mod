package yuki135.blacksouls.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import yuki135.blacksouls.client.render.DevGlassRenderer;

public final class DevGlassButton extends Button {
    public DevGlassButton(int x,int y,int width,int height,Component text,OnPress action) {
        super(x,y,width,height,text,action,DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partialTicks) {
        draw(graphics,getX(),getY(),width,height,getMessage(),active,isHoveredOrFocused());
    }

    public static void draw(GuiGraphics graphics,int x,int y,int width,int height,Component text,boolean active,boolean hovered) {
        hovered &= active;
        DevGlassRenderer.fill(graphics,x,y,width,height,Math.min(8,height / 2F),
                !active ? 0x271C3042 : hovered ? 0x5E31546D : 0x3B1C3042);
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font,text,x+width/2,y+(height-font.lineHeight)/2,
                !active ? 0xFF748090 : hovered ? 0xFFFFFFFF : 0xFFD8E3ED);
    }
}
