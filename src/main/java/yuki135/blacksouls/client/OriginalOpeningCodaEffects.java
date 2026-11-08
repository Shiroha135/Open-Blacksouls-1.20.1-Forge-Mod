package yuki135.blacksouls.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.client.render.OpeningRichFogRenderer;
import yuki135.blacksouls.network.packets.ClientboundOpeningPacket;
import yuki135.blacksouls.story.OriginalOpeningCoda;
import yuki135.blacksouls.story.OriginalOpeningProfile.Stage;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT)
public final class OriginalOpeningCodaEffects {
    private static final ResourceLocation SUNSHINE=ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"textures/gui/opening_coda/sunshine.png");
    private static final ResourceLocation BALLOON=ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"textures/gui/original_npc/balloon.png");
    private static ClientboundOpeningPacket state;
    private static long ticks,receivedAt;
    private static TextureTarget capture;
    private static Ambient ambient;
    private static String ambientName="";

    public record Frame(boolean active,int command,int remainingTicks,int visualState,float tone,float black,
            String picture,double pictureOpacity,boolean pictureBlack,boolean fog,boolean sunshine) { }

    private OriginalOpeningCodaEffects() { }

    public static void accept(ClientboundOpeningPacket packet) {
        boolean newSession=state==null||!state.session().equals(packet.session());
        boolean changed=newSession||state.command()!=packet.command();
        if(newSession) { stopAmbient();receivedAt=ticks; }
        if(!packet.equals(state))receivedAt=ticks;
        state=packet;
        if(OriginalOpeningCoda.active(packet.command())) {
            if(changed)applyAudio(packet.command(),newSession);
        } else {
            releaseCapture();
            if(packet.stage()==Stage.COMPLETE)stopAmbient();
            else if(newSession&&packet.stage()==Stage.FIRST_STEP)playAmbient("MBJH_BGS01",.8F,1F);
        }
    }

    public static Frame frame(float partialTick) {
        if(state==null||!OriginalOpeningCoda.active(state.command()))return new Frame(false,0,0,0,0,0,"",0,false,false,false);
        int command=state.command();
        int elapsed=(int)Math.min(1_000_000,Math.max(0,ticks-receivedAt)*3+Math.max(0,Math.min(2,(int)(partialTick*3))));
        int visual=OriginalOpeningCoda.advanceVisualState(command,state.codaVisualState(),elapsed);
        int remaining=Math.max(0,state.waitTicks()-(int)Math.max(0,ticks-receivedAt));
        var cue=OriginalOpeningCoda.cue(command);
        float black=OriginalOpeningCoda.blackAlpha(command,remaining);
        if(cue.blackFrames()>0) {
            float progress=Math.min(1,(OriginalOpeningCoda.elapsedFrames(command,remaining)+partialTick*3)/cue.blackFrames());
            black=cue.blackFrom()+(cue.blackTo()-cue.blackFrom())*progress;
        }
        double opacity=OriginalOpeningCoda.pictureOpacity(command,remaining);
        if(cue.code()==232&&cue.pictureFrames()>0) {
            double progress=Math.min(1,(OriginalOpeningCoda.elapsedFrames(command,remaining)+partialTick*3)/cue.pictureFrames());
            opacity=cue.pictureFrom()+(cue.pictureTo()-cue.pictureFrom())*progress;
        }
        boolean active=state.notice().isEmpty()&&!state.awaitingReady();
        return new Frame(active,command,remaining,visual,OriginalOpeningCoda.sceneTone(command,remaining,visual),black,
                OriginalOpeningCoda.pictureName(command),opacity,OriginalOpeningCoda.pictureBackgroundBlack(command),
                OriginalOpeningCoda.fogVisible(command),OriginalOpeningCoda.sunshineVisible(command));
    }

    public static boolean controlsHud() {
        return state!=null&&state.locked()&&state.notice().isEmpty()&&OriginalOpeningCoda.active(state.command());
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void hideGameHud(RenderGuiOverlayEvent.Pre event) { if(controlsHud())event.setCanceled(true); }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void hidePlayerHands(RenderHandEvent event) { if(controlsHud())event.setCanceled(true); }

    public static void renderBlackFade(GuiGraphics graphics,float partialTick) {
        var frame=frame(partialTick);
        if(frame.active()&&frame.black()>0)graphics.fill(0,0,graphics.guiWidth(),graphics.guiHeight(),Math.round(Math.min(1,frame.black())*255)<<24);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var minecraft=Minecraft.getInstance();
        if(minecraft.level==null||minecraft.player==null)return;
        if(!minecraft.isPaused()&&(state==null||state.notice().isEmpty()&&!state.awaitingReady()))ticks++;
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void sceneTone(RenderGuiEvent.Pre event) {
        var frame=frame(event.getPartialTick());
        if(!frame.active()||frame.tone()==0||Minecraft.getInstance().level==null)return;
        var graphics=event.getGuiGraphics();
        var shader=OriginalNpcPresentationController.screenShader();
        if(shader==null) { graphics.fill(0,0,graphics.guiWidth(),graphics.guiHeight(),Math.round(-frame.tone())<<24);return; }
        graphics.flush();
        var minecraft=Minecraft.getInstance();var main=minecraft.getMainRenderTarget();
        if(capture==null||capture.width!=main.width||capture.height!=main.height) {
            releaseCapture();capture=new TextureTarget(main.width,main.height,false,Minecraft.ON_OSX);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,capture.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0,0,main.width,main.height,0,0,capture.width,capture.height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
        main.bindWrite(false);
        var previous=RenderSystem.getShader();int previousTexture=RenderSystem.getShaderTexture(0);
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();RenderSystem.disableCull();
            RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,capture.getColorTextureId());shader.setSampler("Sampler0",capture.getColorTextureId());
            float tone=frame.tone()/255F;
            shader.safeGetUniform("Tone").set(tone,tone,tone,0F);shader.safeGetUniform("Flash").set(0F,0F,0F,0F);
            shader.safeGetUniform("Brightness").set(1F);shader.safeGetUniform("Shake").set(0F);
            var buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(-1,-1,0).uv(0,0).endVertex();buffer.vertex(1,-1,0).uv(1,0).endVertex();
            buffer.vertex(1,1,0).uv(1,1).endVertex();buffer.vertex(-1,1,0).uv(0,1).endVertex();BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShader(()->previous);RenderSystem.setShaderTexture(0,previousTexture);RenderSystem.depthMask(mask);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
        }
    }

    @SubscribeEvent(priority=EventPriority.LOW)
    public static void layers(RenderGuiEvent.Pre event) {
        var frame=frame(event.getPartialTick());
        if(!frame.active()) {
            if(state!=null&&(state.stage()==Stage.FIRST_STEP||state.stage()==Stage.COLLAPSE1||state.stage()==Stage.COLLAPSE2||state.stage()==Stage.COLLAPSE3)
                    &&!OriginalOpeningCinematicFog.stopped())OpeningRichFogRenderer.render(event.getGuiGraphics(),event.getPartialTick());
            return;
        }
        var graphics=event.getGuiGraphics();
        if(frame.sunshine())sunshine(graphics,event.getPartialTick());
        if(frame.fog()&&!OriginalOpeningStageEffects.active())OpeningRichFogRenderer.render(graphics,event.getPartialTick());
        if(frame.pictureBlack())graphics.fill(0,0,graphics.guiWidth(),graphics.guiHeight(),0xFF000000);
        var texture=OriginalOpeningCodaAssets.pictureTexture(frame.picture());
        if(texture!=null&&frame.pictureOpacity()>0) {
            var canvas=OriginalNpcPictureController.canvas(graphics.guiWidth(),graphics.guiHeight());
            graphics.flush();graphics.setColor(1,1,1,(float)(frame.pictureOpacity()/255));
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            graphics.blit(texture,(int)canvas.x(),(int)canvas.y(),(int)canvas.width(),(int)canvas.height(),0,0,640,480,640,480);
            graphics.setColor(1,1,1,1);RenderSystem.disableBlend();
        }
        selfBalloon(graphics,frame,event.getPartialTick());
        if(!(Minecraft.getInstance().screen instanceof yuki135.blacksouls.client.gui.GuiOpeningCoda))renderBlackFade(graphics,event.getPartialTick());
    }

    private static void selfBalloon(GuiGraphics graphics,Frame frame,float partialTick) {
        var minecraft=Minecraft.getInstance();
        var cue=OriginalOpeningCoda.cue(frame.command());
        if(cue.code()!=213||cue.parameters().get(0).getAsInt()!=-1||minecraft.getCameraEntity()!=minecraft.player||!minecraft.options.getCameraType().isFirstPerson())return;
        int elapsed=Math.min(cue.durationFrames(),OriginalOpeningCoda.elapsedFrames(frame.command(),frame.remainingTicks())+(int)(partialTick*3));
        int remaining=Math.max(0,76-elapsed);if(remaining==0)return;
        int tile=OriginalNpcPresentationState.balloonFrame(remaining),id=cue.parameters().get(1).getAsInt();
        int size=Math.max(16,Math.round(32*Math.min(graphics.guiWidth()/640F,graphics.guiHeight()/480F)));
        graphics.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        graphics.blit(BALLOON,(graphics.guiWidth()-size)/2,Math.round(graphics.guiHeight()*.24F),size,size,tile*32,(id-1)*32,32,32,256,320);
        RenderSystem.disableBlend();
    }

    private static void sunshine(GuiGraphics graphics,float partialTick) {
        var canvas=OriginalNpcPictureController.canvas(graphics.guiWidth(),graphics.guiHeight());
        float frames=(ticks+partialTick)*3;
        float phase=frames%300,a=phase<150?150-phase:phase-150;
        float third=frames%450,b=third<225?third*2/3:(450-third)*2/3;
        float[] opacities={a,150-a,b};
        graphics.flush();
        for(int index=0;index<3;index++) {
            int cycle=(int)(index==0?(frames+150)/300:index==1?frames/300:frames/450);
            float angle=(index*113+cycle*71)%360;
            graphics.pose().pushPose();graphics.pose().translate(canvas.x()+320*canvas.scale(),-180*canvas.scale(),0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(-angle));graphics.pose().scale((float)canvas.scale(),(float)canvas.scale(),1);
            graphics.setColor(1,1,1,opacities[index]/255);RenderSystem.enableBlend();RenderSystem.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
            graphics.blit(SUNSHINE,-500,-500,1000,1000,0,0,1000,1000,1000,1000);graphics.pose().popPose();
        }
        graphics.setColor(1,1,1,1);RenderSystem.defaultBlendFunc();RenderSystem.disableBlend();
    }

    private static void applyAudio(int command,boolean restoring) {
        var cue=OriginalOpeningCoda.cue(command);
        if(restoring&&OriginalOpeningCoda.memory(command))restoreMemoryMusic(command);
        if(cue.code()==245) {
            var sound=cue.parameters().get(0).getAsJsonObject();
            playAmbient(sound.get("name").getAsString(),sound.get("volume").getAsFloat()/100F,sound.get("pitch").getAsFloat()/100F);
        } else if(cue.code()==246&&ambient!=null)ambient.fade(cue.parameters().get(0).getAsInt()*60);
        else if(restoring&&cue.map()==110) {
            int phase=command-OriginalOpeningCoda.BASE;
            int fadeIndex=switch(cue.page()) { case 1->85;case 2->82;case 3->88;default->0; };
            if(phase>=210&&phase<=214||fadeIndex>0&&cue.sourceIndex()<fadeIndex)playAmbient("川",.5F,.7F);
        }
    }

    private static void restoreMemoryMusic(int command) {
        var current=OriginalOpeningCoda.cue(command);
        OriginalOpeningCoda.Cue latest=null;
        for(int index=0;index<=current.sourceIndex();index++) {
            int candidate=OriginalOpeningCoda.BASE+current.page()*1000+index;
            if(!OriginalOpeningCoda.active(candidate))continue;
            var cue=OriginalOpeningCoda.cue(candidate);
            if(cue.code()==241||cue.code()==242)latest=cue;
        }
        if(latest==null||latest.code()!=241)return;
        var audio=latest.parameters().get(0).getAsJsonObject();
        String sound=OriginalOpeningCodaAssets.music(audio.get("name").getAsString());
        if(sound.isEmpty())return;
        ClientSceneMusic.suspendForPresentation();
        OriginalNpcMusicController.apply(241,sound,java.util.List.of(audio.get("volume").getAsDouble(),audio.get("pitch").getAsDouble()));
    }

    private static void playAmbient(String name,float volume,float pitch) {
        String sound=switch(name) { case "川"->"opening_coda_river";case "MBJH_BGS01"->"opening_coda_night";default->""; };
        if(sound.isEmpty())return;
        if(sound.equals(ambientName)&&ambient!=null&&!ambient.isStopped())return;
        stopAmbient();ambientName=sound;
        ambient=new Ambient(ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,sound),volume,pitch);
        Minecraft.getInstance().getSoundManager().play(ambient);
    }

    private static void stopAmbient() {
        if(ambient!=null)Minecraft.getInstance().getSoundManager().stop(ambient);
        ambient=null;ambientName="";
    }

    private static void releaseCapture() { if(capture!=null) { capture.destroyBuffers();capture=null; } }
    public static void clear() { state=null;ticks=0;receivedAt=0;releaseCapture();stopAmbient(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    private static final class Ambient extends AbstractTickableSoundInstance {
        private final float baseVolume;
        private int fadeFrames,fadeTotal;
        private Ambient(ResourceLocation sound,float volume,float pitch) {
            super(SoundEvent.createVariableRangeEvent(sound),SoundSource.AMBIENT,RandomSource.create());
            this.volume=volume;baseVolume=volume;this.pitch=pitch;looping=true;relative=true;attenuation=Attenuation.NONE;
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            if(Minecraft.getInstance().isPaused())return;
            if(fadeFrames>0) { fadeFrames=Math.max(0,fadeFrames-3);volume=baseVolume*fadeFrames/fadeTotal;if(fadeFrames==0)stop(); }
        }
        private void fade(int frames) { if(frames<=0)stop();else { fadeFrames=frames;fadeTotal=frames; } }
    }

}
