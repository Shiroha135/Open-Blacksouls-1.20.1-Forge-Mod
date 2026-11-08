package yuki135.blacksouls.client;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.client.gui.GuiOpeningChoice;
import yuki135.blacksouls.client.gui.GuiOpeningGreeting;
import yuki135.blacksouls.client.gui.GuiOpeningProfession;
import yuki135.blacksouls.client.gui.GuiOpeningCoda;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundOpeningPacket;
import yuki135.blacksouls.network.packets.ClientboundOpeningPresentationPacket;
import yuki135.blacksouls.network.packets.ServerboundOpeningPacket;
import yuki135.blacksouls.story.OriginalOpeningProfile.Stage;
import yuki135.blacksouls.story.OriginalOpeningGreeting;
import yuki135.blacksouls.story.OriginalOpeningCoda;
import yuki135.blacksouls.story.OriginalOpeningText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT)
public final class OriginalOpeningController {
    private static ClientboundOpeningPacket state;
    private static long ticks,receivedAt,lastReady;
    private static final OriginalOpeningPresentationMailbox PRESENTATIONS=new OriginalOpeningPresentationMailbox();
    private static ClientboundOpeningPresentationPacket pendingPolicy;

    static {
        OriginalOpeningPresentationController.bind(ordinal->act(ServerboundOpeningPacket.Action.NEXT,ordinal),
                ()->act(ServerboundOpeningPacket.Action.SKIP,0),OriginalOpeningCinematicFog::setStopped);
    }

    private OriginalOpeningController() {
    }

    public static void apply(ClientboundOpeningPacket packet) {
        boolean terminal=packet.stage()==Stage.COMPLETE&&!packet.locked()&&!packet.preview();
        if(!PRESENTATIONS.acceptMain(packet.session(),packet.revision(),terminal))return;
        if(state!=null&&!state.session().equals(packet.session())&&OriginalOpeningPresentationController.active())OriginalOpeningPresentationController.abandonSession();
        if(packet.preview()&&(state==null||!state.preview()))OriginalOpeningPresentationController.capturePreviewAudioPolicy();
        if(!packet.equals(state))receivedAt=ticks;
        state=packet;
        OriginalOpeningCodaEffects.accept(packet);
        var pending=PRESENTATIONS.drain();
        if(pending!=null)applyAcceptedPresentation(pending);
        finishIfReady();
        refreshScreen();
    }

    public static void applyPresentation(ClientboundOpeningPresentationPacket packet) {
        if(packet.policyOnly()) {
            var player=Minecraft.getInstance().player;
            if(player==null)pendingPolicy=packet;
            else if(player.getUUID().equals(packet.session()))OriginalOpeningPresentationController.restoreInitialRoomPolicy(packet.silentRoom());
            return;
        }
        if(state!=null&&state.session().equals(packet.session())&&state.revision()==packet.revision()&&packet.preview()!=state.preview()&&!packet.finished())return;
        if(PRESENTATIONS.offer(packet)==OriginalOpeningPresentationMailbox.Decision.ACCEPT)applyAcceptedPresentation(packet);
    }

    private static void applyAcceptedPresentation(ClientboundOpeningPresentationPacket packet) {
        if(state==null||!state.session().equals(packet.session())||state.revision()!=packet.revision())return;
        if(packet.preview()!=state.preview()&&!packet.finished())return;
        var origin=packet.origin();
        var snapshot=new OriginalOpeningPresentationEngine.Snapshot(packet.episode(),packet.sequence(),packet.program(),packet.command(),packet.elapsedFrames(),
                packet.restoring(),packet.awaitingDestination(),packet.finished(),packet.skipped(),packet.dimension().toString(),origin.getX(),origin.getY(),origin.getZ());
        OriginalOpeningPresentationController.applyNow(snapshot,packet.preview());
        OriginalOpeningPresentationController.restoreInitialRoomSilence(packet.silentRoom());
        if(PRESENTATIONS.applied(packet))finishIfReady();
        refreshScreen();
    }

    private static void finishIfReady() {
        if(!PRESENTATIONS.canClose())return;
        PRESENTATIONS.close();state=null;
        OriginalOpeningCodaEffects.clear();
        if(Minecraft.getInstance().screen instanceof GuiOpeningChoice || Minecraft.getInstance().screen instanceof GuiOpeningGreeting
                || Minecraft.getInstance().screen instanceof GuiOpeningProfession || Minecraft.getInstance().screen instanceof GuiOpeningCoda)Minecraft.getInstance().setScreen(null);
    }

    public static ClientboundOpeningPacket state() { return state; }
    public static int waitTicks() { return state==null?0:Math.max(0,state.waitTicks()-(int)(ticks-receivedAt)); }
    public static boolean professionVisible(int origin) {
        if(origin>0&&!OriginalOpeningGreeting.selectable(origin))return false;
        if(origin>0&&state!=null&&OriginalOpeningCoda.active(state.command()))return OriginalOpeningCoda.professionsPresent(state.command());
        if (origin<1 || origin>9 || state==null || state.stage()!=Stage.ORIGIN_SELECTION || !OriginalOpeningGreeting.active(state.command())) return true;
        if (!OriginalOpeningGreeting.animation(state.command())) return state.command()==OriginalOpeningGreeting.LAST_COMMAND;
        return OriginalOpeningGreeting.animationFrame(waitTicks(),origin)>=0;
    }
    public static void act(ServerboundOpeningPacket.Action action,int value) {
        if (state!=null) NetworkHandler.INSTANCE.sendToServer(new ServerboundOpeningPacket(state.session(),state.revision(),action,value));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        var minecraft=Minecraft.getInstance();
        if (minecraft.level==null || minecraft.player==null) return;
        if(pendingPolicy!=null) { var policy=pendingPolicy;pendingPolicy=null;if(minecraft.player.getUUID().equals(policy.session()))OriginalOpeningPresentationController.restoreInitialRoomPolicy(policy.silentRoom()); }
        if (minecraft.isPaused()) return;
        ticks++;
        if (state==null) return;
        if (state.awaitingReady() && ticks-lastReady>=10 && minecraft.level.dimension().location().equals(state.dimension())
                && minecraft.level.hasChunkAt(state.anchor()) && minecraft.player.position().distanceToSqr(state.anchor().getCenter())<9) {
            lastReady=ticks;
            act(ServerboundOpeningPacket.Action.READY,0);
        }
        refreshScreen();
    }

    private static void refreshScreen() {
        if (state==null) return;
        var minecraft=Minecraft.getInstance();
        if(state.awaitingReady()&&state.notice().isEmpty()) {
            if(minecraft.screen instanceof GuiOpeningCoda screen&&OriginalOpeningCoda.active(state.command()))screen.accept(state);
            else if(minecraft.screen instanceof GuiOpeningChoice||minecraft.screen instanceof GuiOpeningGreeting||minecraft.screen instanceof GuiOpeningProfession||minecraft.screen instanceof GuiOpeningCoda)minecraft.setScreen(null);
            return;
        }
        boolean greeting=state.stage()==Stage.ORIGIN_SELECTION && OriginalOpeningGreeting.active(state.command())
                && !state.awaitingReady() && state.notice().isEmpty() && !OriginalOpeningPresentationController.locksInput() && !PRESENTATIONS.terminalPending();
        if (greeting) {
            if (minecraft.screen instanceof GuiOpeningGreeting screen) screen.accept(state);
            else if (minecraft.screen==null || minecraft.screen instanceof GuiOpeningChoice || minecraft.screen instanceof GuiOpeningCoda) minecraft.setScreen(new GuiOpeningGreeting(state));
            return;
        }
        if(OriginalOpeningCoda.active(state.command())&&!state.awaitingReady()&&state.notice().isEmpty()&&!PRESENTATIONS.terminalPending()) {
            if(minecraft.screen instanceof GuiOpeningCoda screen)screen.accept(state);
            else if(minecraft.screen==null||minecraft.screen instanceof GuiOpeningChoice||minecraft.screen instanceof GuiOpeningProfession||minecraft.screen instanceof GuiOpeningGreeting)
                minecraft.setScreen(new GuiOpeningCoda(state));
            return;
        }
        if(state.stage()==Stage.ORIGIN_SELECTION && state.focusOrigin()>0 && state.notice().isEmpty()
                && !state.awaitingReady() && !OriginalOpeningPresentationController.locksInput() && !PRESENTATIONS.terminalPending()) {
            if(minecraft.screen instanceof GuiOpeningProfession screen)screen.accept(state);
            else if(minecraft.screen==null || minecraft.screen instanceof GuiOpeningChoice || minecraft.screen instanceof GuiOpeningGreeting)
                minecraft.setScreen(new GuiOpeningProfession(state));
            return;
        }
        boolean dialog=OriginalOpeningPresentationController.locksInput() || PRESENTATIONS.terminalPending() || !state.notice().isEmpty() || !state.awaitingReady()
                && (state.stage()==Stage.ORIGIN_SELECTION&&state.focusOrigin()>0 || state.stage()==Stage.ORIGIN_CONFIRMED || state.stage()==Stage.ALICE_SELECTION);
        if (dialog) {
            if (minecraft.screen instanceof GuiOpeningChoice choice) choice.accept(state);
            else if (minecraft.screen==null || minecraft.screen instanceof GuiOpeningGreeting || minecraft.screen instanceof GuiOpeningProfession || minecraft.screen instanceof GuiOpeningCoda) minecraft.setScreen(new GuiOpeningChoice(state));
        } else if (minecraft.screen instanceof GuiOpeningChoice || minecraft.screen instanceof GuiOpeningGreeting || minecraft.screen instanceof GuiOpeningProfession || minecraft.screen instanceof GuiOpeningCoda) minecraft.setScreen(null);
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        if (!PRESENTATIONS.terminalPending()&&!OriginalOpeningPresentationController.locksInput() && (state==null || !state.locked())) return;
        var input=event.getInput();
        input.forwardImpulse=0;
        input.leftImpulse=0;
        input.up=false;
        input.down=false;
        input.left=false;
        input.right=false;
        input.jumping=false;
        input.shiftKeyDown=false;
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        if (state==null || OriginalOpeningPresentationController.active()) return;
        var minecraft=Minecraft.getInstance();
        if (minecraft.player==null) return;
        var graphics=event.getGuiGraphics();
        int width=graphics.guiWidth(),height=graphics.guiHeight();
        if(OriginalOpeningCoda.active(state.command())&&state.notice().isEmpty()) {
            if(state.awaitingReady())graphics.fill(0,0,width,height,0xFF000000);
            return;
        }
        if(PRESENTATIONS.terminalPending()) { graphics.fill(0,0,width,height,0xFF000000);return; }
        int alpha=state.awaitingReady()||!state.notice().isEmpty()?255:state.stage()==Stage.MEMORY_DIALOGUE?255:state.stage()==Stage.NAME_PENDING?255:0;
        if (state.stage()==Stage.FALLING) alpha=255;
        if (alpha>0) graphics.fill(0,0,width,height,alpha<<24);
        String text="";
        if (!state.notice().isEmpty()) text=state.notice();
        else if (state.awaitingReady()) text="";
        else if (state.stage()==Stage.ORIGIN_SELECTION&&state.focusOrigin()==0&&!OriginalOpeningGreeting.active(state.command())) text="走近人物并交谈，选择你的出身。";
        else if (state.stage()==Stage.COLLAPSE1||state.stage()==Stage.COLLAPSE2) text="脚下的土地正在崩落……";
        else if (state.stage()==Stage.COMPLETE&&state.preview()) text="正在结束回放……";
        else if (state.stage()==Stage.FALLING) {
            int wait=waitTicks();
            text=wait>120?OriginalOpeningText.poem(0):wait>60?OriginalOpeningText.poem(1):OriginalOpeningText.title();
        }
        if (!text.isEmpty()) {
            int top=alpha>0?height/2-18:height-44;
            for (var line:minecraft.font.split(Component.literal(text),Math.max(60,width-60))) {
                graphics.drawCenteredString(minecraft.font,line,width/2,top,0xFFF2EEE4);
                top+=12;
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { state=null;pendingPolicy=null;ticks=0;receivedAt=0;lastReady=0;PRESENTATIONS.clear();OriginalOpeningPresentationController.clear();OriginalOpeningCinematicFog.setStopped(false);OriginalOpeningCodaEffects.clear(); }
}
