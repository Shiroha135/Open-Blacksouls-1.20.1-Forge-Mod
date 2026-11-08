package yuki135.blacksouls.client.gui;

import yuki135.blacksouls.client.ClientStoryName;
import yuki135.blacksouls.client.OriginalOpeningController;
import yuki135.blacksouls.client.render.FadedBannerRenderer;
import yuki135.blacksouls.network.packets.ClientboundOpeningPacket;
import yuki135.blacksouls.network.packets.ServerboundOpeningPacket.Action;
import yuki135.blacksouls.story.OriginalOpeningGreeting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class GuiOpeningGreeting extends Screen {
    private ClientboundOpeningPacket state;
    private int visibleCharacters;
    private int ticks;
    private boolean advanceKeyDown;
    private boolean pauseReleased;

    public GuiOpeningGreeting(ClientboundOpeningPacket state) {
        super(Component.empty());
        this.state=state;
    }

    public void accept(ClientboundOpeningPacket next) {
        if (!state.session().equals(next.session()) || state.command()!=next.command()) {
            visibleCharacters=0;
            ticks=0;
            pauseReleased=false;
        }
        state=next;
    }

    private String text() {
        String name=minecraft==null || minecraft.player==null?"":ClientStoryName.get(minecraft.player);
        return OriginalOpeningGreeting.message(state.command(),name);
    }

    @Override
    public void tick() {
        if (OriginalOpeningGreeting.animation(state.command())) return;
        String text=text();
        if(paused(text))return;
        if (++ticks%2==0 && visibleCharacters<text.codePointCount(0,text.length())) visibleCharacters++;
    }

    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        if (OriginalOpeningGreeting.animation(state.command())) return;
        String text=text();
        int characters=text.codePointCount(0,text.length());
        int end=text.offsetByCodePoints(0,Math.min(visibleCharacters,characters));
        int textWidth=Math.max(32,width-56);
        int lines=font.split(Component.literal(text),textWidth).size();
        int bannerHeight=Math.min(height,Math.max(90,lines*12+40));
        int top=(height-bannerHeight)/2;
        FadedBannerRenderer.draw(graphics,0,top,width,top+bannerHeight);
        int lineTop=top+(bannerHeight-((lines-1)*12+font.lineHeight))/2;
        for (var line:font.split(Component.literal(text.substring(0,end)),textWidth)) {
            graphics.drawString(font,line,28,lineTop,0xFFFFFFFF,false);
            lineTop+=12;
        }
        if(paused(text)||visibleCharacters>=characters) {
            int center=width/2,y=top+bannerHeight-8;
            for(int row=0;row<4;row++)graphics.fill(center-3+row,y+row,center+4-row,y+row+1,0xFFFFFFFF);
        }
    }

    private void advance() {
        if (OriginalOpeningGreeting.animation(state.command())) return;
        String text=text();
        int characters=text.codePointCount(0,text.length());
        if(paused(text)) { pauseReleased=true;return; }
        if (visibleCharacters<characters) {
            visibleCharacters=pauseReleased?characters:Math.min(characters,pauseAt(text));
            return;
        }
        if (OriginalOpeningController.waitTicks()==0) {
            OriginalOpeningController.act(Action.NEXT,OriginalOpeningGreeting.page(state.command()));
        }
    }

    private int pauseAt(String text) {
        if(state.command()!=OriginalOpeningGreeting.SECOND_COMMAND)return Integer.MAX_VALUE;
        int index=text.indexOf("\n　这样肯定");
        return index<0?Integer.MAX_VALUE:text.codePointCount(0,index);
    }

    private boolean paused(String text) { return !pauseReleased&&visibleCharacters>=pauseAt(text); }

    @Override
    public boolean mouseClicked(double mouseX,double mouseY,int button) {
        if (button==GLFW.GLFW_MOUSE_BUTTON_LEFT) advance();
        return true;
    }

    @Override
    public boolean keyPressed(int key,int scanCode,int modifiers) {
        if (key==GLFW.GLFW_KEY_ENTER || key==GLFW.GLFW_KEY_KP_ENTER || key==GLFW.GLFW_KEY_SPACE) {
            if (!advanceKeyDown) advance();
            advanceKeyDown=true;
        }
        return true;
    }

    @Override
    public boolean keyReleased(int key,int scanCode,int modifiers) {
        if (key==GLFW.GLFW_KEY_ENTER || key==GLFW.GLFW_KEY_KP_ENTER || key==GLFW.GLFW_KEY_SPACE) advanceKeyDown=false;
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() { }
}
