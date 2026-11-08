package yuki135.blacksouls.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import yuki135.blacksouls.client.OriginalOpeningController;
import yuki135.blacksouls.client.render.FadedBannerRenderer;
import yuki135.blacksouls.network.packets.ClientboundOpeningPacket;
import yuki135.blacksouls.network.packets.ServerboundOpeningPacket.Action;
import yuki135.blacksouls.story.OriginalOpeningText;
import yuki135.blacksouls.util.OriginalNpcTextures;
import yuki135.blacksouls.util.OriginalOpeningData;

public final class GuiOpeningProfession extends Screen {
    private ClientboundOpeningPacket state;
    private boolean confirmation;
    private boolean keyDown;
    private boolean submitted;
    private int visibleCharacters;
    private int ticks;
    private int selected;

    public GuiOpeningProfession(ClientboundOpeningPacket state) {
        super(Component.empty());
        this.state=state;
    }

    public void accept(ClientboundOpeningPacket next) {
        if (!state.session().equals(next.session()) || state.focusOrigin()!=next.focusOrigin()) {
            confirmation=false;
            submitted=false;
            visibleCharacters=0;
            ticks=0;
            selected=0;
        }
        state=next;
    }

    private List<String> description() {
        return OriginalOpeningData.get(state.focusOrigin()).sourceDescription();
    }

    private String text() {
        var lines=description();
        return OriginalOpeningText.plain(confirmation?lines.get(lines.size()-1):String.join("\n",lines.subList(0,lines.size()-1)));
    }

    private boolean finished() {
        String text=text();
        return visibleCharacters>=text.codePointCount(0,text.length());
    }

    private float scale() { return Math.min(width/640F,height/480F); }
    private boolean hasPortrait() { return OriginalNpcTextures.portrait(OriginalOpeningData.get(state.focusOrigin()).actor().faceName())!=null; }
    private int boxWidth() { return Math.max(64,Math.round(120*scale())); }
    private int rowHeight() { return Math.max(12,Math.round(24*scale())); }
    private int boxHeight() { return rowHeight()*2+Math.max(8,Math.round(24*scale())); }
    private int boxLeft() { return width-boxWidth()-2; }
    private int boxTop() { return Math.min(height-boxHeight()-2,(height+bannerHeight())/2+Math.round(6*scale())); }

    private int bannerHeight() {
        float textScale=2*scale();
        int textLeft=confirmation||!hasPortrait()?Math.round(12*scale()):Math.round(116*scale());
        int lines=font.split(Component.literal(text()),Math.max(1,(int)((width-textLeft-12*scale())/textScale))).size();
        return Math.max(Math.round(108*scale()),Math.round((lines*12+8)*textScale));
    }

    @Override
    public void tick() {
        if (++ticks%2==0 && !finished()) visibleCharacters++;
    }

    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        float scale=scale(),textScale=scale*2;
        int bannerHeight=bannerHeight(),top=(height-bannerHeight)/2;
        FadedBannerRenderer.draw(graphics,0,top,width,top+bannerHeight);
        if (!confirmation) {
            var actor=OriginalOpeningData.get(state.focusOrigin()).actor();
            var portrait=OriginalNpcTextures.portrait(actor.faceName());
            if (portrait!=null) {
                int size=Math.round(96*scale);
                graphics.blit(OriginalNpcTextures.texture(portrait),Math.round(8*scale),top+(bannerHeight-size)/2,size,size,
                        actor.faceIndex()%4*96,actor.faceIndex()/4*96,96,96,portrait.get("width").getAsInt(),portrait.get("height").getAsInt());
            }
        }
        String text=text();
        int end=text.offsetByCodePoints(0,Math.min(visibleCharacters,text.codePointCount(0,text.length())));
        MutableComponent visible=Component.empty();
        if (confirmation) visible.append(text.substring(0,end));
        else {
            int titleEnd=text.indexOf('\n');
            if (titleEnd<0) titleEnd=text.length();
            visible.append(Component.literal(text.substring(0,Math.min(end,titleEnd))).withStyle(Style.EMPTY.withColor(0xFF7848)));
            if (end>titleEnd) visible.append(text.substring(titleEnd,end));
        }
        int left=confirmation||!hasPortrait()?Math.round(12*scale):Math.round(116*scale);
        int textWidth=Math.max(1,(int)((width-left-12*scale)/textScale));
        int y=top+Math.round(10*scale);
        graphics.pose().pushPose();
        graphics.pose().scale(textScale,textScale,1);
        for (FormattedCharSequence line:font.split(visible,textWidth)) {
            graphics.drawString(font,line,Math.round(left/textScale),Math.round(y/textScale),0xFFFFFFFF,false);
            y+=Math.round(12*textScale);
        }
        graphics.pose().popPose();
        if (confirmation && finished()) {
            int x=boxLeft(),boxY=boxTop(),padding=(boxHeight()-rowHeight()*2)/2;
            BSGuiUtils.drawRMWindow(graphics,x,boxY,boxWidth(),boxHeight(),Math.max(4,Math.round(8*scale)));
            for (int index=0;index<2;index++) {
                int rowY=boxY+padding+index*rowHeight();
                if (index==selected) BSGuiUtils.drawRMSelection(graphics,x+padding/2,rowY,boxWidth()-padding,rowHeight());
                graphics.pose().pushPose();
                graphics.pose().scale(textScale,textScale,1);
                graphics.drawString(font,index==0?"是":"否",Math.round((x+padding)/textScale),Math.round((rowY+(rowHeight()-font.lineHeight*textScale)/2)/textScale),0xFFFFFFFF,false);
                graphics.pose().popPose();
            }
        } else if (!confirmation && finished()) {
            int x=width/2,y1=top+bannerHeight-Math.max(4,Math.round(7*scale));
            for(int row=0;row<Math.max(3,Math.round(5*scale));row++)graphics.fill(x-3+row,y1+row,x+4-row,y1+row+1,0xFFFFFFFF);
        }
    }

    private void advance() {
        if (submitted) return;
        if (!finished()) { String text=text();visibleCharacters=text.codePointCount(0,text.length());return; }
        if (!confirmation) { confirmation=true;visibleCharacters=0;ticks=0;return; }
        if (OriginalOpeningController.waitTicks()>0) return;
        submitted=true;
        OriginalOpeningController.act(selected==0?Action.CONFIRM_ORIGIN:Action.CLOSE_PREVIEW,selected==0?state.focusOrigin():0);
    }

    @Override
    public boolean mouseClicked(double mouseX,double mouseY,int button) {
        if(button!=GLFW.GLFW_MOUSE_BUTTON_LEFT)return true;
        if(confirmation && finished()) {
            int padding=(boxHeight()-rowHeight()*2)/2;
            int row=(int)Math.floor((mouseY-boxTop()-padding)/rowHeight());
            if(mouseX<boxLeft()||mouseX>=boxLeft()+boxWidth()||row<0||row>1)return true;
            selected=row;
        }
        advance();return true;
    }

    @Override
    public boolean keyPressed(int key,int scanCode,int modifiers) {
        if(key==GLFW.GLFW_KEY_ESCAPE) { onClose();return true; }
        if(key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_DOWN) { if(confirmation&&finished())selected=1-selected;return true; }
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER||key==GLFW.GLFW_KEY_SPACE) {
            if(!keyDown)advance();keyDown=true;
        }
        return true;
    }

    @Override
    public boolean keyReleased(int key,int scanCode,int modifiers) {
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER||key==GLFW.GLFW_KEY_SPACE)keyDown=false;
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public void onClose() { if(!submitted) { submitted=true;OriginalOpeningController.act(Action.CLOSE_PREVIEW,0); } }
}
