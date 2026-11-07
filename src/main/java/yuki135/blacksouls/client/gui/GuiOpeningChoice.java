package yuki135.blacksouls.client.gui;

import yuki135.blacksouls.client.OriginalOpeningController;
import yuki135.blacksouls.client.OriginalOpeningPresentationController;
import yuki135.blacksouls.network.packets.ClientboundOpeningPacket;
import yuki135.blacksouls.network.packets.ServerboundOpeningPacket.Action;
import yuki135.blacksouls.story.OriginalOpeningProfile.Stage;
import yuki135.blacksouls.story.OriginalOpeningText;
import yuki135.blacksouls.util.OriginalOpeningData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiOpeningChoice extends Screen {
    private ClientboundOpeningPacket state;
    private final List<Button> delayed=new ArrayList<>();
    private double scroll;
    private int contentHeight;
    private boolean cinematicActive;
    private int cinematicPage=-1;

    public GuiOpeningChoice(ClientboundOpeningPacket state) {
        super(Component.literal("故事的开端"));
        this.state=state;
    }

    public void accept(ClientboundOpeningPacket next) {
        boolean rebuild=state.stage()!=next.stage()||state.focusOrigin()!=next.focusOrigin()||state.command()!=next.command()||state.preview()!=next.preview()||!state.notice().equals(next.notice());
        state=next;
        if (rebuild) rebuildWidgets();
    }

    @Override
    protected void init() {
        delayed.clear();
        scroll=0;
        contentHeight=0;
        cinematicActive=OriginalOpeningPresentationController.locksInput();
        cinematicPage=OriginalOpeningPresentationController.messageOrdinal();
        int left=width/2-90,top=Math.max(50,height-98);
        if (!state.notice().isEmpty()) {
            button("重新准备",left,top,180,Action.RETRY,0,false);
            if (state.preview()) button("返回原处",width-96,height-26,84,Action.EXIT_REPLAY,0,false);
            return;
        }
        if(cinematicActive) {
            if(OriginalOpeningPresentationController.messageOpen())button("继续",left,top,180,Action.NEXT,Math.max(0,cinematicPage),true);
            button("跳过演出",12,height-26,84,Action.SKIP,0,false);
            if(state.preview())button("返回原处",width-96,height-26,84,Action.EXIT_REPLAY,0,false);
            return;
        }
        if (state.stage()==Stage.ORIGIN_SELECTION) {
            button("以此出身开始",left,top,180,Action.CONFIRM_ORIGIN,state.focusOrigin(),true);
            button("再看看其他人",left,top+24,180,Action.CLOSE_PREVIEW,0,false);
        } else if (state.stage()==Stage.ALICE_SELECTION) {
            button("女儿爱丽丝",left,top-24,180,Action.RELATIONSHIP,1,true);
            button("妹妹爱丽丝",left,top,180,Action.RELATIONSHIP,2,true);
            button("母亲爱丽丝",left,top+24,180,Action.RELATIONSHIP,3,true);
        } else if (state.stage()==Stage.ORIGIN_CONFIRMED) {
            if (state.command()==27) {
                button("有点困了",left,top,86,Action.NEXT,1,true);
                button("还不困",left+94,top,86,Action.NEXT,2,true);
            } else button("继续",left,top,180,Action.NEXT,0,true);
        }
        button("跳过演出",12,height-26,84,Action.SKIP,0,false);
        if (state.preview()) button("返回原处",width-96,height-26,84,Action.EXIT_REPLAY,0,false);
    }

    private void button(String label,int left,int top,int width,Action action,int value,boolean wait) {
        var button=addRenderableWidget(Button.builder(Component.literal(label),ignored->{
            if(OriginalOpeningPresentationController.locksInput()&&action==Action.NEXT)OriginalOpeningPresentationController.advance();
            else OriginalOpeningController.act(action,value);
        }).bounds(left,top,width,20).build());
        if (wait) delayed.add(button);
    }

    @Override
    public void tick() {
        if(cinematicActive!=OriginalOpeningPresentationController.locksInput()||cinematicPage!=OriginalOpeningPresentationController.messageOrdinal())rebuildWidgets();
        delayed.forEach(button->button.active=OriginalOpeningPresentationController.locksInput()?OriginalOpeningPresentationController.frame().canAdvance():OriginalOpeningController.waitTicks()==0);
    }

    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        if(OriginalOpeningPresentationController.locksInput()&&state.notice().isEmpty()) {
            OriginalOpeningPresentationController.drawOverlay(graphics);
            super.render(graphics,mouseX,mouseY,partialTick);
            return;
        }
        graphics.fill(0,0,width,height,0xB8000000);
        int left=Math.max(12,width/2-180),textWidth=Math.max(100,Math.min(360,width-24));
        int top=Math.max(16,height/2-100);
        if (!state.notice().isEmpty()) {
            paragraph(graphics,state.notice(),left,Math.max(30,height/2-38),textWidth,0xFFF3F0E8);
        } else if (state.stage()==Stage.ORIGIN_SELECTION) {
            var profession=OriginalOpeningData.get(state.focusOrigin());
            graphics.drawCenteredString(font,OriginalOpeningText.plain(profession.sourceLabel()),width/2,20,0xFFF5E6C5);
            int contentTop=42,contentBottom=Math.max(contentTop+12,height-122);
            graphics.enableScissor(left,contentTop,left+textWidth,contentBottom);
            top=contentTop-(int)scroll;
            for (int index=1;index<profession.sourceDescription().size()-1;index++) {
                top=paragraph(graphics,OriginalOpeningText.plain(profession.sourceDescription().get(index)),left,top,textWidth,0xFFE5E0D8);
            }
            var parameters=profession.sourceInitialEffectiveParameters();
            String[] names={"生命","魔力","攻击","防御","魔攻","魔防","敏捷","幸运"};
            for (int row=0;row<2;row++) {
                var text=new StringBuilder();
                for (int column=0;column<4;column++) { int index=row*4+column;if(column>0)text.append("    ");text.append(names[index]).append(' ').append(parameters.get(index)); }
                top=paragraph(graphics,text.toString(),left,top,textWidth,0xFFF3F0E8);
            }
            var items=new ArrayList<String>();
            for(var slot:profession.initialEquipment()) if(slot.id()!=0) {
                var item=OriginalOpeningData.catalog().initialEquipment(slot.kind(),slot.id());
                if(item!=null)items.add(item.name());
            }
            top=paragraph(graphics,"初始装备："+(items.isEmpty()?"无":String.join("、",items)),left,top+5,textWidth,0xFFE5E0D8);
            top=paragraph(graphics,"初始技能："+String.join("、",profession.initialSkillIds().stream().map(OriginalOpeningText::skillName).toList()),left,top+5,textWidth,0xFFE5E0D8);
            if (profession.extraSouls()>0) top=paragraph(graphics,"额外获得 "+profession.extraSouls()+" 魂",left,top+5,textWidth,0xFFE3D392);
            var extras=profession.extraRewards().stream().filter(reward->reward.kind()==OriginalOpeningData.RewardKind.ITEM)
                    .map(reward->reward.name()+" ×"+reward.amount()).toList();
            if (!extras.isEmpty()) top=paragraph(graphics,"随身物品："+String.join("、",extras),left,top+5,textWidth,0xFFE5E0D8);
            contentHeight=top+(int)scroll-contentTop;
            graphics.disableScissor();
            if (contentHeight>contentBottom-contentTop) graphics.drawString(font,"滚动查看详情",left,contentBottom+4,0xFFBDB6A9,false);
        } else if (state.stage()==Stage.ALICE_SELECTION) {
            graphics.drawCenteredString(font,"在你身边的是……",width/2,Math.max(24,height/2-48),0xFFF5E6C5);
        } else if (state.stage()==Stage.ORIGIN_CONFIRMED) {
            paragraph(graphics,OriginalOpeningText.sleep(state.command()),left,Math.max(30,height/2-38),textWidth,0xFFF3F0E8);
        }
        super.render(graphics,mouseX,mouseY,partialTick);
    }

    private int paragraph(GuiGraphics graphics,String text,int left,int top,int width,int color) {
        for(var line:font.split(Component.literal(text),width)) { graphics.drawString(font,line,left,top,color,false);top+=12; }
        return top;
    }

    @Override
    public boolean keyPressed(int key,int scanCode,int modifiers) {
        if(OriginalOpeningPresentationController.locksInput()&&OriginalOpeningPresentationController.keyPressed(key,org.lwjgl.glfw.GLFW.GLFW_PRESS))return true;
        return super.keyPressed(key,scanCode,modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX,double mouseY,int button) {
        if(super.mouseClicked(mouseX,mouseY,button))return true;
        return OriginalOpeningPresentationController.locksInput()&&OriginalOpeningPresentationController.mousePressed(button,org.lwjgl.glfw.GLFW.GLFW_PRESS);
    }

    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double amount) {
        if (state.stage()==Stage.ORIGIN_SELECTION&&state.notice().isEmpty()) {
            int available=Math.max(12,height-164);
            scroll=net.minecraft.util.Mth.clamp(scroll-amount*18.0D,0.0D,Math.max(0,contentHeight-available));
            return true;
        }
        return super.mouseScrolled(mouseX,mouseY,amount);
    }

    @Override
    public boolean isPauseScreen() { return false; }
    @Override
    public boolean shouldCloseOnEsc() { return state.stage()==Stage.ORIGIN_SELECTION; }
    @Override
    public void onClose() {
        if (state.stage()==Stage.ORIGIN_SELECTION) OriginalOpeningController.act(Action.CLOSE_PREVIEW,0);
    }
}
