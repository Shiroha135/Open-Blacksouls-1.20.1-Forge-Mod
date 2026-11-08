package yuki135.blacksouls.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;
import yuki135.blacksouls.client.ClientStoryName;
import yuki135.blacksouls.client.OriginalOpeningCodaAssets;
import yuki135.blacksouls.client.OriginalOpeningController;
import yuki135.blacksouls.client.render.FadedBannerRenderer;
import yuki135.blacksouls.network.packets.ClientboundOpeningPacket;
import yuki135.blacksouls.network.packets.ServerboundOpeningPacket.Action;
import yuki135.blacksouls.story.OriginalOpeningCoda;
import yuki135.blacksouls.story.OriginalOpeningPresentationData;

public final class GuiOpeningCoda extends Screen {
    public static final float WINDOW_BACKGROUND_OPACITY=192.0F/255.0F;
    private ClientboundOpeningPacket state;
    private String rawText="";
    private String completedMessageText="";
    private Message message=Message.parse("");
    private int visibleCharacters;
    private int selected;
    private int waitFrames;
    private boolean fastReveal;
    private boolean advanceKeyDown;
    private boolean submitted;
    private final Set<Integer> releasedPauses=new HashSet<>();
    private final Set<Integer> consumedWaits=new HashSet<>();

    public GuiOpeningCoda(ClientboundOpeningPacket state) {
        super(Component.empty());
        this.state=state;
        resetMessage(false);
    }

    public void accept(ClientboundOpeningPacket next) {
        boolean changed=!state.session().equals(next.session())||state.command()!=next.command();
        boolean sameSession=state.session().equals(next.session());
        state=next;
        if(changed)resetMessage(sameSession);
    }

    private void resetMessage(boolean preserveChoiceContext) {
        String previous=rawText;
        if(preserveChoiceContext&&finished()&&!previous.isEmpty())completedMessageText=previous;
        else if(!preserveChoiceContext)completedMessageText="";
        rawText=OriginalOpeningCoda.message(state.command());
        var player=net.minecraft.client.Minecraft.getInstance().player;
        String name=player==null?"":ClientStoryName.get(player);
        rawText=rawText.replace("\\n[1]",name.replace("\\","\\\\")).replace("\\N[1]",name.replace("\\","\\\\"));
        message=Message.parse(rawText);
        boolean sourceChoice=!choices().isEmpty()&&OriginalOpeningCoda.cue(state.command()).code()==102;
        boolean carried=preserveChoiceContext&&!choices().isEmpty()&&(previous.equals(rawText)||completedMessageText.equals(rawText));
        visibleCharacters=sourceChoice||carried?message.characters():0;
        releasedPauses.clear();consumedWaits.clear();
        if(sourceChoice||carried) { releasedPauses.addAll(message.pauses());consumedWaits.addAll(message.waits().keySet()); }
        waitFrames=0;fastReveal=false;selected=0;submitted=false;
    }

    private List<String> choices() { return OriginalOpeningCoda.choices(state.command()); }
    private boolean inputEnabled() { return !submitted&&!state.awaitingReady()&&state.notice().isEmpty()&&OriginalOpeningCoda.acceptsInput(state.command()); }
    private boolean finished() { return visibleCharacters==message.characters()&&!paused()&&waitFrames==0
            &&(!message.waits().containsKey(visibleCharacters)||consumedWaits.contains(visibleCharacters)); }
    private boolean paused() { return message.pauses().contains(visibleCharacters)&&!releasedPauses.contains(visibleCharacters); }
    private float scale() { return Math.max(.25F,Math.min(width/640F,height/480F)); }
    private float textScale() { return scale()*2; }
    private int sourcePhase() { return state.command()-OriginalOpeningCoda.BASE; }
    private boolean bottom() { return OriginalOpeningCoda.messagePosition(state.command())==2; }
    private boolean window() { return OriginalOpeningCoda.messageStyle(state.command())==0; }
    private OriginalOpeningCodaAssets.Portrait portrait() { return OriginalOpeningCodaAssets.portrait(OriginalOpeningCoda.faceName(state.command()),OriginalOpeningCoda.faceIndex(state.command())); }
    private boolean hasPortrait() { return portrait()!=null; }
    private int canvasLeft() { return Math.round((width-640*scale())/2); }
    private int canvasWidth() { return Math.round(640*scale()); }
    private int textLeft() { return (window()?canvasLeft():0)+Math.round((hasPortrait()?124:12)*scale()); }
    private int textWidth() { return Math.max(1,Math.round(((window()?canvasLeft()+canvasWidth():width)-textLeft()-12*scale())/textScale())); }
    private int bannerHeight() { return Math.max(Math.round(120*scale()),Math.round((font.split(message.component(message.characters()),textWidth()).size()*24+24)*scale())); }
    private int bannerTop() { return bottom()?height-bannerHeight():(height-bannerHeight())/2; }
    private int rowHeight() { return Math.max(12,Math.round(24*scale())); }
    private int choiceWidth() {
        int text=choices().stream().mapToInt(value->font.width(Message.parse(value).component(Integer.MAX_VALUE))).max().orElse(0);
        return Math.min(width-Math.round(12*scale()),Math.max(Math.round(120*scale()),Math.round(text*textScale()+24*scale())));
    }
    private int choiceHeight() { return rowHeight()*choices().size()+Math.round(24*scale()); }
    private int choiceLeft() { return (bottom()?canvasLeft()+canvasWidth():width)-choiceWidth(); }
    private int choiceTop() {
        int proposed=bottom()?bannerTop()-choiceHeight():bannerTop()+bannerHeight()+Math.round(4*scale());
        return Math.max(0,Math.min(height-choiceHeight(),proposed));
    }

    @Override
    public void tick() {
        if(!inputEnabled())return;
        for(int frame=0;frame<3;frame++) {
            if(waitFrames>0) { waitFrames--;continue; }
            Integer wait=message.waits().get(visibleCharacters);
            if(wait!=null&&consumedWaits.add(visibleCharacters)) { waitFrames=Math.max(0,wait-1);continue; }
            if(finished()||paused())return;
            if(fastReveal)revealToBoundary();
            else if(visibleCharacters<message.characters())visibleCharacters++;
        }
    }

    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        if(state.awaitingReady()&&state.notice().isEmpty()) { graphics.fill(0,0,width,height,0xFF000000);return; }
        yuki135.blacksouls.client.OriginalOpeningCodaEffects.renderBlackFade(graphics,partialTick);
        if(message.plain().isEmpty()&&choices().isEmpty())return;
        float scale=scale(),textScale=textScale();
        int top=bannerTop(),bannerHeight=bannerHeight();
        if(window())BSGuiUtils.drawRMWindow(graphics,canvasLeft(),top,canvasWidth(),bannerHeight,Math.max(4,Math.round(16*scale)),Math.max(16,Math.round(64*scale)),WINDOW_BACKGROUND_OPACITY);
        else FadedBannerRenderer.draw(graphics,0,top,width,top+bannerHeight);
        String face=OriginalOpeningCoda.faceName(state.command());
        var portrait=portrait();
        if(portrait!=null) {
            int index=OriginalOpeningCoda.faceIndex(state.command()),size=Math.round(96*scale);
            graphics.blit(portrait.texture(),(window()?canvasLeft():0)+Math.round(12*scale),top+Math.round(12*scale),size,size,
                    index%portrait.columns()*portrait.frameWidth(),index/portrait.columns()*portrait.frameHeight(),
                    portrait.frameWidth(),portrait.frameHeight(),portrait.width(),portrait.height());
        }
        String speaker=OriginalOpeningCodaAssets.speaker(face);
        if(!speaker.isEmpty()) {
            int nameWidth=Math.round(font.width(speaker)*textScale+16*scale),nameHeight=Math.round(24*scale);
            int nameLeft=(window()?canvasLeft():0)+Math.round(12*scale),nameTop=Math.max(0,top-nameHeight+Math.round(4*scale));
            BSGuiUtils.drawRMWindow(graphics,nameLeft,nameTop,nameWidth,nameHeight,Math.max(3,Math.round(8*scale)),Math.max(16,Math.round(64*scale)),WINDOW_BACKGROUND_OPACITY);
            drawText(graphics,Component.literal(speaker),nameLeft+Math.round(8*scale),nameTop+Math.round(3*scale),textScale);
        }
        int y=top+Math.round(12*scale);
        for(var line:font.split(message.component(visibleCharacters),textWidth())) {
            graphics.pose().pushPose();graphics.pose().translate(textLeft(),y,1);graphics.pose().scale(textScale,textScale,1);
            graphics.drawString(font,line,0,0,0xFFFFFFFF,false);graphics.pose().popPose();y+=Math.round(24*scale);
        }
        if(finished()&&inputEnabled()&&!choices().isEmpty())drawChoices(graphics);
        else if(inputEnabled()&&(finished()||paused())) {
            int x=width/2,y1=top+bannerHeight-Math.round(8*scale),size=Math.max(3,Math.round(5*scale));
            for(int row=0;row<size;row++)graphics.fill(x-size+row,y1+row,x+size-row,y1+row+1,0xFFFFFFFF);
        }
    }

    private void drawChoices(GuiGraphics graphics) {
        float scale=scale(),textScale=textScale();
        int x=choiceLeft(),y=choiceTop(),padding=Math.round(12*scale);
        BSGuiUtils.drawRMWindow(graphics,x,y,choiceWidth(),choiceHeight(),Math.max(4,Math.round(16*scale)),Math.max(16,Math.round(64*scale)),WINDOW_BACKGROUND_OPACITY);
        for(int index=0;index<choices().size();index++) {
            int rowY=y+padding+index*rowHeight();
            if(index==selected) {
                BSGuiUtils.drawRMSelection(graphics,x+padding,rowY,choiceWidth()-padding*2,rowHeight());
            }
            drawText(graphics,Message.parse(choices().get(index)).component(Integer.MAX_VALUE),x+padding,rowY+Math.round(2*scale),textScale);
        }
    }

    private void drawText(GuiGraphics graphics,Component text,int x,int y,float scale) {
        graphics.pose().pushPose();graphics.pose().translate(x,y,1);graphics.pose().scale(scale,scale,1);
        graphics.drawString(font,text,0,0,0xFFFFFFFF,false);graphics.pose().popPose();
    }

    private void advance() {
        if(!inputEnabled())return;
        if(waitFrames>0)return;
        if(paused()) { releasedPauses.add(visibleCharacters);fastReveal=false;return; }
        if(!finished()) {
            fastReveal=true;revealToBoundary();return;
        }
        submitted=true;
        boolean relationship=OriginalOpeningCoda.aliceChoice(state.command());
        OriginalOpeningController.act(relationship?Action.RELATIONSHIP:Action.NEXT,choices().isEmpty()?0:selected+1);
    }

    private void revealToBoundary() {
        int end=message.characters();
        for(int pause:message.pauses())if(pause>=visibleCharacters&&!releasedPauses.contains(pause)) { end=pause;break; }
        for(int wait:message.waits().keySet())if(wait>=visibleCharacters&&!consumedWaits.contains(wait))end=Math.min(end,wait);
        visibleCharacters=end;
    }

    private int hoveredChoice(double mouseX,double mouseY) {
        if(!finished()||!inputEnabled()||choices().isEmpty()||mouseX<choiceLeft()||mouseX>=choiceLeft()+choiceWidth())return -1;
        int row=(int)Math.floor((mouseY-choiceTop()-Math.round(12*scale()))/rowHeight());
        return row>=0&&row<choices().size()?row:-1;
    }

    @Override public void mouseMoved(double mouseX,double mouseY) { int row=hoveredChoice(mouseX,mouseY);if(row>=0)selected=row; }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button) {
        if(button!=GLFW.GLFW_MOUSE_BUTTON_LEFT)return true;
        if(finished()&&!choices().isEmpty()) { int row=hoveredChoice(mouseX,mouseY);if(row<0)return true;selected=row; }
        advance();return true;
    }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers) {
        if(key==GLFW.GLFW_KEY_ESCAPE&&state.preview()&&(modifiers&GLFW.GLFW_MOD_CONTROL)!=0) {
            if(!submitted) { submitted=true;OriginalOpeningController.act(Action.EXIT_REPLAY,0); }return true;
        }
        if(key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_DOWN) {
            if(finished()&&!choices().isEmpty())selected=Math.floorMod(selected+(key==GLFW.GLFW_KEY_UP?-1:1),choices().size());
            return true;
        }
        if(key==GLFW.GLFW_KEY_ESCAPE) { if((sourcePhase()==6||sourcePhase()==28)&&finished()) { selected=choices().size()-1;advance(); }return true; }
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER||key==GLFW.GLFW_KEY_SPACE) { if(!advanceKeyDown)advance();advanceKeyDown=true; }
        return true;
    }
    @Override public boolean keyReleased(int key,int scanCode,int modifiers) {
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER||key==GLFW.GLFW_KEY_SPACE)advanceKeyDown=false;
        return true;
    }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { }

    private record Message(String plain,List<Integer> colors,List<Integer> pauses,Map<Integer,Integer> waits) {
        private static Message parse(String source) {
            var plain=new StringBuilder();var colors=new ArrayList<Integer>();var pauses=new ArrayList<Integer>();var waits=new HashMap<Integer,Integer>();
            int color=0xFFFFFFFF;
            for(int index=0;index<source.length();) {
                int point=source.codePointAt(index);index+=Character.charCount(point);
                if(point=='\\'&&index<source.length()) {
                    char escape=source.charAt(index++);
                    if((escape=='c'||escape=='C')&&index<source.length()&&source.charAt(index)=='[') {
                        int end=source.indexOf(']',index+1);
                        if(end>=0) { color=OriginalOpeningPresentationData.color(Integer.parseInt(source.substring(index+1,end)));index=end+1;continue; }
                    }
                    if(escape=='!') { pauses.add(colors.size());continue; }
                    if(escape=='.'||escape=='|') { waits.merge(colors.size(),escape=='.'?15:60,Integer::sum);continue; }
                    if(escape=='{'||escape=='}'||escape=='>'||escape=='<'||escape=='^'||escape=='$')continue;
                    point=escape;
                }
                plain.appendCodePoint(point);colors.add(color);
            }
            return new Message(plain.toString(),List.copyOf(colors),List.copyOf(pauses),Map.copyOf(waits));
        }
        private int characters() { return colors.size(); }
        private MutableComponent component(int limit) {
            int count=Math.min(characters(),limit),offset=0;
            var result=Component.empty();
            for(int index=0;index<count;) {
                int color=colors.get(index),end=index+1;
                while(end<count&&colors.get(end)==color)end++;
                int next=plain.offsetByCodePoints(offset,end-index);
                result.append(Component.literal(plain.substring(offset,next)).withStyle(Style.EMPTY.withColor(color&0xFFFFFF)));
                index=end;offset=next;
            }
            return result;
        }
    }
}
