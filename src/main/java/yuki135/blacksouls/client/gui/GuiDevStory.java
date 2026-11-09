package yuki135.blacksouls.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yuki135.blacksouls.client.render.DevGlassRenderer;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ServerboundDevStoryPacket;

public final class GuiDevStory extends Screen {
    private final Screen parent;
    private int left,top,w,h,mode,listScroll,textScroll,request,lastReceived;
    private String category="switches",selected="",token="",details="正在读取世界剧情状态……";
    private JsonObject catalog=new JsonObject();
    private EditBox search,value,pointName;
    private Button apply;
    private final List<Button> actions=new ArrayList<>();
    private boolean waiting;

    public GuiDevStory(Screen parent) {super(Component.literal("剧情与事件测试"));this.parent=parent;}
    @Override protected void init() {
        w=Math.min(760,width-12);h=Math.min(430,height-12);left=(width-w)/2;top=(height-h)/2;
        button("进度预设 / 测试点",left+10,top+29,122,18,()->mode(0));
        button("开关 / 变量",left+136,top+29,92,18,()->mode(1));
        button("事件页 / 对话",left+232,top+29,92,18,()->mode(2));
        button("刷新",left+w-98,top+7,40,18,()->send("overview","","",""));
        button("返回",left+w-54,top+7,44,18,this::onClose);
        search=addRenderableWidget(new EditBox(font,left+10,top+55,Math.min(166,w-20),18,Component.literal("搜索编号、原作名称")));
        search.setMaxLength(100);search.setHint(Component.literal("搜索编号、原作名称"));search.setResponder(ignored->listScroll=0);
        value=addRenderableWidget(new EditBox(font,left+190,top+h-65,Math.max(50,w-200),18,Component.literal("目标值")));value.setMaxLength(24);
        value.setResponder(ignored->invalidate());
        pointName=addRenderableWidget(new EditBox(font,left+10,top+56,Math.max(80,w-20),18,Component.literal("测试点备注")));pointName.setMaxLength(40);pointName.setHint(Component.literal("测试点备注，如：兔洞猫赠礼后"));
        apply=button("应用预览",left+w-112,top+h-26,102,18,()->{if(!token.isEmpty())send("apply","","",token);});
        button("撤回上次修改",left+10,top+h-26,112,18,()->send("undo","","",""));
        rebuildActions();send("overview","","","");
    }
    private Button button(String label,int x,int y,int width,int height,Runnable action) {return addRenderableWidget(Button.builder(Component.literal(label),ignored->action.run()).bounds(x,y,width,height).build());}
    private void action(String text,int x,int y,int width,Runnable runnable){actions.add(button(text,x,y,width,18,runnable));}
    private void invalidate(){request++;waiting=false;token="";}
    private void mode(int next){invalidate();mode=next;selected="";listScroll=0;textScroll=0;search.setValue("");details=summary();rebuildActions();}
    private void rebuildActions() {
        for(var button:actions)removeWidget(button);actions.clear();
        search.visible=mode>0;value.visible=mode==1;pointName.visible=mode==0;
        if(mode==0) {
            int bw=(w-24)/2;
            action("猎颅兽：战前对白",left+10,top+82,bw,()->preview("beast_before",""));
            action("猎颅兽：战后对白",left+14+bw,top+82,bw,()->preview("beast_after",""));
            for(int slot=1;slot<=3;slot++) {
                final int index=slot;int y=top+108+(slot-1)*25;
                action("保存到 "+slot,left+10,y,82,()->send("save",Integer.toString(index),pointName.getValue(),""));
                action("预览读取 "+slot,left+96,y,94,()->preview("load:"+index,""));
            }
        } else if(mode==1) {
            action("S 开关",left+180,top+55,62,()->{invalidate();category="switches";selected="";listScroll=0;});
            action("V 变量",left+246,top+55,62,()->{invalidate();category="variables";selected="";listScroll=0;});
            action("预览目标值",left+132,top+h-26,96,()->{if(!selected.isEmpty())preview(selected,value.getValue().strip());});
            action("开/关",left+180,top+h-88,58,()->{var row=selectedRow();if(row!=null&&selected.startsWith("S:")) {value.setValue(row.get("value").getAsString().equals("true")?"false":"true");}});
        } else {
            for(int i=0;i<4;i++) {String key="ABCD".substring(i,i+1);int x=left+180+i*40;action(key+"开",x,top+55,38,()->{if(!selected.isEmpty())preview("self:"+selected+":"+key,"true");});}
            for(int i=0;i<4;i++) {String key="ABCD".substring(i,i+1);int x=left+180+i*40;action(key+"关",x,top+76,38,()->{if(!selected.isEmpty())preview("self:"+selected+":"+key,"false");});}
            action("显示事件",left+180,top+h-65,70,()->{if(!selected.isEmpty())preview("hidden:"+selected,"false");});
            action("隐藏事件",left+254,top+h-65,70,()->{if(!selected.isEmpty())preview("hidden:"+selected,"true");});
            action("清除本人续接",left+180,top+h-44,130,()->{if(!selected.isEmpty())preview("resume:"+selected,"");});
        }
    }
    private String summary() {
        String progress=catalog.has("progress")?catalog.get("progress").getAsString():"?";
        String message="主线 V11 = "+progress+"\n世界共享：同存档所有玩家都会受影响。\n预设只改对白条件，不模拟通关、奖励或Boss复活。\n测试点保存开关、变量、独立开关、隐藏与Boss记录；\n背包/奖励/职业/个人SEN/誓约不回退。\n修改先预览，再应用；可撤回上次修改。";
        if(catalog.has("points"))for(int i=0;i<3;i++)message+="\n测试点 "+(i+1)+"："+catalog.getAsJsonArray("points").get(i).getAsString();
        return message;
    }
    private void preview(String key,String value){send("preview",key,value,"");}
    private void send(String action,String key,String value,String token){waiting=true;NetworkHandler.sendToServer(new ServerboundDevStoryPacket(++request,action,key,value,token));}
    public void receive(int revision,String data) {
        if(revision!=request||revision<lastReceived)return;lastReceived=revision;waiting=false;
        var response=JsonParser.parseString(data).getAsJsonObject();
        if(response.has("catalog"))catalog=response;
        if(response.has("message"))details=response.get("message").getAsString();
        if(response.has("token"))token=response.get("token").getAsString();else token="";
        if(response.has("catalog"))details=summary()+"\n\n"+details;
        textScroll=0;
    }
    private List<JsonObject> rows() {
        if(mode==0)return List.of();String key=mode==2?"events":category;
        if(!catalog.has(key))return List.of();String filter=search.getValue().strip().toLowerCase(Locale.ROOT);
        return catalog.getAsJsonArray(key).asList().stream().map(e->e.getAsJsonObject()).filter(row->(row.get("name").getAsString()+" "+row.get("key").getAsString()).toLowerCase(Locale.ROOT).contains(filter)).toList();
    }
    private JsonObject selectedRow(){return rows().stream().filter(row->row.get("key").getAsString().equals(selected)).findFirst().orElse(null);}
    private int rowCount(){return Math.max(1,(h-124)/20);}
    private int textTop(){return mode==0?top+182:mode==2?top+100:top+80;}
    private int textBottom(){return top+h-(mode==2?72:mode==1?96:34);}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        DevGlassRenderer.beginFrame(graphics);graphics.fill(0,0,width,height,0x80000000);DevGlassRenderer.panel(graphics,left,top,w,h,12,.95F,true);
        graphics.drawString(font,"剧情与事件测试 · 世界共享",left+10,top+10,0xFFFFFFFF,false);
        if(mode>0) {
            var rows=rows();listScroll=Mth.clamp(listScroll,0,Math.max(0,rows.size()-rowCount()));
            graphics.enableScissor(left+8,top+80,left+174,top+h-34);
            for(int i=0;i<rowCount()&&i+listScroll<rows.size();i++) {
                var row=rows.get(i+listScroll);int y=top+81+i*20;boolean current=selected.equals(row.get("key").getAsString());
                if(current)graphics.fill(left+9,y-1,left+173,y+18,0x704A7798);
                String text=row.get("name").getAsString();if(row.has("value"))text+=" = "+row.get("value").getAsString();
                graphics.drawString(font,font.plainSubstrByWidth(text,158),left+12,y+4,0xFFE7ECF5,false);
            }
            graphics.disableScissor();
        }
        int textX=mode==0?left+10:left+180,textW=mode==0?w-20:w-190;
        int start=textTop(),bottom=textBottom();
        var wrapped=new ArrayList<net.minecraft.util.FormattedCharSequence>();for(String line:details.split("\n",-1))wrapped.addAll(font.split(Component.literal(line.isEmpty()?" ":line),Math.max(32,textW)));
        int visible=Math.max(1,(bottom-start-12)/12);textScroll=Mth.clamp(textScroll,0,Math.max(0,wrapped.size()-visible));
        graphics.enableScissor(textX,start,textX+textW,Math.max(start+1,bottom));
        for(int i=0;i<visible&&i+textScroll<wrapped.size();i++)graphics.drawString(font,wrapped.get(i+textScroll),textX,start+i*12,0xFFE7ECF5,false);
        graphics.disableScissor();
        if(wrapped.size()>visible)graphics.drawString(font,"滚轮查看详情 "+(textScroll+1)+"/"+wrapped.size(),textX,Math.max(start,bottom-10),0xFFB5C3D1,false);
        if(waiting)graphics.drawString(font,"读取中…",left+w-165,top+12,0xFFFFCC70,false);
        apply.active=!waiting&&!token.isEmpty();super.render(graphics,mouseX,mouseY,partialTick);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(mode>0&&button==0&&x>=left+10&&x<left+174&&y>=top+81&&y<top+81+rowCount()*20) {
            var rows=rows();int index=listScroll+(int)((y-top-81)/20);
            if(index<rows.size()) {invalidate();var row=rows.get(index);selected=row.get("key").getAsString();textScroll=0;
                if(mode==2)send("inspect",selected,"","");else {value.setValue(row.get("value").getAsString());details=row.get("name").getAsString()+"\n当前值="+row.get("value").getAsString()+"\n"+(row.get("readonly").getAsBoolean()?"个人/职业系统拥有：只读。":"输入目标值再预览，开关用true或false。")+"\n世界开关/变量修改会影响所有玩家。";}}
            return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(mode>0&&x<left+176)listScroll=Math.max(0,listScroll-(int)Math.signum(delta)*3);else textScroll=Math.max(0,textScroll-(int)Math.signum(delta)*3);return true;
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){DevGlassRenderer.release();super.removed();}
}
