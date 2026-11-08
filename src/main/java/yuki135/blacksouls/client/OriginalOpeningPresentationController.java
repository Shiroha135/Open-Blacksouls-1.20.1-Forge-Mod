package yuki135.blacksouls.client;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.story.OriginalOpeningPresentationData;
import yuki135.blacksouls.story.OriginalOpeningScenes;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPresentationPacket;
import yuki135.blacksouls.client.gui.GuiOpeningChoice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT)
public final class OriginalOpeningPresentationController {
    public static final int MAX_DEBRIS_CELLS=48;
    public static final int PARTICLES_PER_CELL=2;
    private static final OriginalOpeningPresentationEngine ENGINE=new OriginalOpeningPresentationEngine();
    private static final ResourceLocation BALLOON=ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"textures/gui/original_npc/balloon.png");
    private static TextureTarget capture;
    private static ShaderInstance screenShader;
    private static Ambient ambient;
    private static String ambientKey="";
    private static boolean ownsSceneMusic;
    private static boolean silentArrival;
    private static boolean pendingRoomTone;
    private static boolean previewPolicyCaptured,previewWasSilent;
    private static boolean lastWorldReady;
    private static Consumer<Integer> advanceRequest=ignored->{};
    private static Runnable skipRequest=()->{};
    private static Consumer<Boolean> fogRequest=ignored->{};

    private OriginalOpeningPresentationController() {
    }

    public static void bind(Consumer<Integer> advance,Runnable skip,Consumer<Boolean> stopFog) {
        advanceRequest=Objects.requireNonNull(advance);skipRequest=Objects.requireNonNull(skip);fogRequest=Objects.requireNonNull(stopFog);
    }

    public static void capturePreviewAudioPolicy() { if(!previewPolicyCaptured) { previewWasSilent=silentArrival;previewPolicyCaptured=true;OriginalOpeningAmbientController.capturePreview(); } }

    public static void restoreInitialRoomPolicy(boolean quiet) {
        pendingRoomTone=quiet;
        if(!quiet)restoreInitialRoomSilence(false);
    }

    public static void restoreInitialRoomSilence(boolean quiet) {
        silentArrival=quiet;
        if(quiet) { ClientSceneMusic.suspendForPresentation();ownsSceneMusic=true; }
        else if(!ENGINE.frame().active()&&ownsSceneMusic) { ClientSceneMusic.resumeAfterPresentation();ownsSceneMusic=false; }
    }

    public static void abandonSession() {
        ENGINE.clear();stopAmbient();OriginalOpeningAmbientController.finish(false);
        previewPolicyCaptured=false;previewWasSilent=false;lastWorldReady=false;fogRequest.accept(false);
        if(capture!=null) { capture.destroyBuffers();capture=null; }
        if(ownsSceneMusic&&!silentArrival) { ClientSceneMusic.resumeAfterPresentation();ownsSceneMusic=false; }
    }

    public static void apply(OriginalOpeningPresentationEngine.Snapshot snapshot,boolean preview) {
        Minecraft.getInstance().execute(()->applyNow(snapshot,preview));
    }

    static void applyNow(OriginalOpeningPresentationEngine.Snapshot snapshot,boolean preview) {
            var before=ENGINE.snapshot();
            var effects=ENGINE.apply(snapshot);
            if(ENGINE.snapshot()!=snapshot)return;
            if(before==null||!before.episode().equals(snapshot.episode()))stopAmbient();
            if(snapshot.finished()) { finish(preview);return; }
            OriginalOpeningAmbientController.begin(preview);
            if(!ownsSceneMusic) { ClientSceneMusic.suspendForPresentation();ownsSceneMusic=true; }
            for(var effect:effects) {
                switch(effect.code()) {
                    case 250 -> playOneShot(effect);
                    case 245 -> { OriginalOpeningAmbientController.stopSourceBgs();syncAmbient(); }
                    case 246 -> { if(ambient!=null)ambient.fade(effect.args().get(0).intValue()*60); }
                    case 225 -> spawnDebris(snapshot);
                    case 355 -> fogRequest.accept(true);
                    default -> { }
                }
            }
            syncAmbient();
            if(snapshot.restoring()||before==null) {
                syncAmbient();
                if(ambient!=null)ambient.restoreGain((float)ENGINE.ambientGain(),ENGINE.ambientFadeFrames());
            }
            if(snapshot.skipped())stopAmbient();
    }

    public static boolean keyPressed(int key,int action) {
        if(action==GLFW.GLFW_PRESS&&key==GLFW.GLFW_KEY_ESCAPE&&locksInput()&&ENGINE.requestSkip()) { skipRequest.run();return true; }
        if(action!=GLFW.GLFW_PRESS||(key!=GLFW.GLFW_KEY_ENTER&&key!=GLFW.GLFW_KEY_KP_ENTER&&key!=GLFW.GLFW_KEY_SPACE))return false;
        return advance();
    }

    public static boolean mousePressed(int button,int action) {
        if(button!=GLFW.GLFW_MOUSE_BUTTON_LEFT||action!=GLFW.GLFW_PRESS)return false;
        return advance();
    }

    public static boolean advance() { if(!ENGINE.requestAdvance()&&!ENGINE.requestReveal())return false;advanceRequest.accept(ENGINE.messageOrdinal());return true; }

    public static OriginalOpeningPresentationEngine.Frame frame() { return ENGINE.frame(); }
    public static boolean active() { return ENGINE.frame().active(); }
    public static boolean messageOpen() { return ENGINE.frame().page()!=null&&!ENGINE.frame().awaitingDestination(); }
    public static int messageOrdinal() { return ENGINE.messageOrdinal(); }
    public static boolean locksInput() {
        var snapshot=ENGINE.snapshot();
        return ENGINE.frame().active()&&snapshot!=null&&(snapshot.awaitingDestination()||snapshot.program().equals("finale99")||snapshot.program().equals("arrival"));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var minecraft=Minecraft.getInstance();
        if(pendingRoomTone&&minecraft.level!=null&&minecraft.player!=null
                &&minecraft.level.dimension().location().toString().equals("blacksouls:hokoniwa")&&ClientSceneState.getSceneId().equals("map_001")
                &&minecraft.level.hasChunkAt(minecraft.player.blockPosition())) {
            OriginalNpcPresentationController.apply(new ClientboundOriginalNpcPresentationPacket(223,minecraft.player.getId(),minecraft.level.dimension().location(),
                    List.of(-34.0D,-34.0D,-34.0D,0.0D,0.0D),""));
            restoreInitialRoomSilence(true);
            pendingRoomTone=false;
        }
        if(silentArrival&&minecraft.level!=null&&minecraft.level.dimension().location().toString().equals("blacksouls:hokoniwa")&&ClientSceneState.getSceneId().equals("map_001")) {
            ClientSceneMusic.suspendForPresentation();ownsSceneMusic=true;
        }
        if(silentArrival&&minecraft.level!=null&&!ENGINE.frame().active()&&(!minecraft.level.dimension().location().toString().equals("blacksouls:hokoniwa")||!ClientSceneState.getSceneId().equals("map_001"))) {
            ClientSceneMusic.resumeAfterPresentation();ownsSceneMusic=false;
        }
        if(ENGINE.snapshot()==null)return;
        if(minecraft.isPaused())return;
        var snapshot=ENGINE.snapshot();
        boolean ready=minecraft.level!=null&&minecraft.player!=null&&minecraft.level.dimension().location().toString().equals(snapshot.dimension())
                &&minecraft.level.hasChunkAt(minecraft.player.blockPosition());
        lastWorldReady=ready;
        ENGINE.advanceClientFrames(3,ready);
        if(ready&&ENGINE.ambient()==null&&ambient!=null)stopAmbient();
        if(ready&&ambient!=null&&ambient.started&&!ambient.isStopped()&&!minecraft.getSoundManager().isActive(ambient)) {
            ambient.started=false;minecraft.getSoundManager().play(ambient);
        }
    }

    @SubscribeEvent
    public static void renderScreen(RenderGuiEvent.Pre event) {
        var frame=renderFrame();
        if(!frame.active()||screenShader==null||Minecraft.getInstance().level==null)return;
        event.getGuiGraphics().flush();
        var minecraft=Minecraft.getInstance();
        RenderTarget main=minecraft.getMainRenderTarget();
        if(capture==null||capture.width!=main.width||capture.height!=main.height) {
            if(capture!=null)capture.destroyBuffers();
            capture=new TextureTarget(main.width,main.height,false,Minecraft.ON_OSX);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,capture.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0,0,main.width,main.height,0,0,capture.width,capture.height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
        main.bindWrite(false);
        var previous=RenderSystem.getShader();
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),blend=GL11.glIsEnabled(GL11.GL_BLEND);
        boolean mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int texture=RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();RenderSystem.disableCull();
            RenderSystem.setShader(()->screenShader);
            screenShader.setSampler("Sampler0",capture.getColorTextureId());
            RenderSystem.setShaderTexture(0,capture.getColorTextureId());
            var tone=frame.tone();var flash=frame.flash();
            screenShader.safeGetUniform("Tone").set((float)(tone[0]/255),(float)(tone[1]/255),(float)(tone[2]/255),(float)(tone[3]/255));
            screenShader.safeGetUniform("Flash").set((float)(flash[0]/255),(float)(flash[1]/255),(float)(flash[2]/255),(float)(flash[3]/255));
            screenShader.safeGetUniform("Brightness").set(frame.brightness()/255F);
            screenShader.safeGetUniform("Shake").set((float)(frame.shake()/640));
            BufferBuilder buffer=Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(-1,-1,0).uv(0,0).endVertex();buffer.vertex(1,-1,0).uv(1,0).endVertex();
            buffer.vertex(1,1,0).uv(1,1).endVertex();buffer.vertex(-1,1,0).uv(0,1).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShader(()->previous);RenderSystem.setShaderTexture(0,texture);RenderSystem.depthMask(mask);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
        }
    }

    @SubscribeEvent
    public static void renderText(RenderGuiEvent.Post event) {
        if(Minecraft.getInstance().screen instanceof GuiOpeningChoice)return;
        drawOverlay(event.getGuiGraphics());
    }

    public static void drawOverlay(GuiGraphics graphics) {
        var frame=renderFrame();
        if(!frame.active())return;
        int width=graphics.guiWidth(),height=graphics.guiHeight();
        var tone=frame.tone();
        if(frame.awaitingDestination()||frame.brightness()==0||tone[0]<=-255&&tone[1]<=-255&&tone[2]<=-255)graphics.fill(0,0,width,height,0xFF000000);
        var page=frame.page();
        if(page!=null&&!frame.awaitingDestination()) {
            var minecraft=Minecraft.getInstance();
            float canvas=Math.min(width/640F,height/480F);
            float top=(height-120*canvas)*.5F+12*canvas;
            float lineTop=0;
            var glyphs=page.visible(frame.messageFrames());
            for(int line=0;line<page.lines();line++) {
                final int currentLine=line;
                var visible=glyphs.stream().filter(glyph->glyph.line()==currentLine).toList();
                var full=page.glyphs().stream().filter(glyph->glyph.line()==currentLine).toList();
                float total=0;
                for(var glyph:full)total+=minecraft.font.width(glyph.text())*glyph.size()/9F*canvas;
                float x=page.title()?(width-total)*.5F:(width-640*canvas)*.5F+12*canvas;
                for(var glyph:visible) {
                    float scale=glyph.size()/9F*canvas;
                    graphics.pose().pushPose();graphics.pose().translate(x,top+lineTop*canvas,0);graphics.pose().scale(scale,scale,1);
                    graphics.drawString(minecraft.font,Component.literal(glyph.text()),0,0,glyph.color(),false);
                    graphics.pose().popPose();x+=minecraft.font.width(glyph.text())*scale;
                }
                lineTop+=Math.max(page.title()?22:24,full.stream().mapToInt(OriginalOpeningPresentationMessage.Glyph::size).max().orElse(22));
            }
            if(frame.canAdvance()) {
                graphics.pose().pushPose();graphics.pose().translate(width*.5F,(height-120*canvas)*.5F+112*canvas,0);graphics.pose().scale(canvas,canvas,1);
                for(int row=0;row<4;row++)graphics.fill(-3+row,row,4-row,row+1,0xFFFFFFFF);
                graphics.pose().popPose();
            }
        }
        if(frame.balloon()>0&&!frame.awaitingDestination()) {
            int size=Math.max(16,Math.round(32*Math.min(width/640F,height/480F)));
            graphics.pose().pushPose();graphics.pose().translate((width-size)*.5F,height*.24F,0);graphics.pose().scale(size/32F,size/32F,1);
            graphics.blit(BALLOON,0,0,frame.balloonFrame()*32,(frame.balloon()-1)*32,32,32,256,320);
            graphics.pose().popPose();
        }
    }

    private static void playOneShot(OriginalOpeningPresentationEngine.Effect effect) {
        if(effect.sound().isEmpty())return;
        var sound=new SimpleSoundInstance(Objects.requireNonNull(ResourceLocation.tryParse(effect.sound())),SoundSource.NEUTRAL,
                effect.args().get(0).floatValue()/100F,effect.args().get(1).floatValue()/100F,RandomSource.create(),false,0,
                SoundInstance.Attenuation.NONE,0,0,0,true);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void syncAmbient() {
        var directive=ENGINE.ambient();
        String key=directive==null?"":directive.sound();
        if(key.equals(ambientKey)&&ambient!=null&&!ambient.isStopped())return;
        stopAmbient();ambientKey=key;
        if(directive!=null&&!key.isEmpty()) {
            ambient=new Ambient(Objects.requireNonNull(ResourceLocation.tryParse(key)),directive.args().get(0).floatValue()/100F,directive.args().get(1).floatValue()/100F);
            Minecraft.getInstance().getSoundManager().play(ambient);
        }
    }

    private static void stopAmbient() {
        if(ambient!=null)Minecraft.getInstance().getSoundManager().stop(ambient);
        ambient=null;ambientKey="";
    }

    private static void spawnDebris(OriginalOpeningPresentationEngine.Snapshot snapshot) {
        var minecraft=Minecraft.getInstance();
        if(minecraft.level==null||!minecraft.level.dimension().location().toString().equals(snapshot.dimension()))return;
        OriginalOpeningScenes.SceneKind before,after;
        switch(snapshot.program()) {
            case "collapse110" -> { before=OriginalOpeningScenes.SceneKind.ISLAND_110;after=OriginalOpeningScenes.SceneKind.ISLAND_97; }
            case "collapse97" -> { before=OriginalOpeningScenes.SceneKind.ISLAND_97;after=OriginalOpeningScenes.SceneKind.ISLAND_98; }
            case "collapse98" -> { before=OriginalOpeningScenes.SceneKind.ISLAND_98;after=OriginalOpeningScenes.SceneKind.ISLAND_99; }
            default -> { return; }
        }
        var removed=new ArrayList<OriginalOpeningScenes.Cell>();
        for(int z=0;z<24;z++)for(int x=0;x<30;x++) {
            var cell=new OriginalOpeningScenes.Cell(x,z);
            if(OriginalOpeningScenes.sourceSupport(before,cell)&&!OriginalOpeningScenes.sourceSupport(after,cell))removed.add(cell);
        }
        removed.sort(Comparator.comparingLong(cell->mix(snapshot.episode().getLeastSignificantBits()^cell.x()*31L^cell.y()*8191L)));
        var random=RandomSource.create(snapshot.episode().getMostSignificantBits()^snapshot.program().hashCode());
        var fallback=ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"original_opening_flower_grass"));
        for(var cell:removed.subList(0,Math.min(MAX_DEBRIS_CELLS,removed.size()))) {
            var position=new BlockPos(snapshot.originX()+cell.x(),snapshot.originY()-1,snapshot.originZ()+cell.y());
            if(!minecraft.level.hasChunkAt(position))continue;
            var state=minecraft.level.getBlockState(position);
            if(state.isAir())state=fallback==null||fallback==Blocks.AIR?Blocks.GRASS_BLOCK.defaultBlockState():fallback.defaultBlockState();
            for(int index=0;index<PARTICLES_PER_CELL;index++)minecraft.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,state),
                    position.getX()+.2+random.nextDouble()*.6,position.getY()+.7,position.getZ()+.2+random.nextDouble()*.6,
                    (random.nextDouble()-.5)*.12,-.08-random.nextDouble()*.05,(random.nextDouble()-.5)*.12);
        }
    }

    private static long mix(long value) { value^=value>>>33;value*=0xff51afd7ed558ccdl;value^=value>>>33;return value; }
    private static OriginalOpeningPresentationEngine.Frame renderFrame() { return ENGINE.renderFrame(lastWorldReady&&!Minecraft.getInstance().isPaused()?Minecraft.getInstance().getFrameTime():0); }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void music(PlaySoundEvent event) {
        var sound=event.getSound();
        if(sound==null||sound.getSource()!=SoundSource.MUSIC||TurnBattleAudioGate.isActive())return;
        var minecraft=Minecraft.getInstance();
        boolean quiet=silentArrival&&minecraft.level!=null&&minecraft.level.dimension().location().toString().equals("blacksouls:hokoniwa")&&ClientSceneState.getSceneId().equals("map_001");
        if(ENGINE.frame().active()||quiet&&ClientSceneMusic.isSceneSound(sound.getLocation())&&sound.getClass().getEnclosingClass()!=OriginalNpcMusicController.class)event.setSound(null);
    }
    @SubscribeEvent public static void ambientStarted(PlayStreamingSourceEvent event) { if(event.getSound()==ambient)ambient.started=true; }
    private static void finish(boolean preview) {
        var minecraft=Minecraft.getInstance();
        boolean quiet=(!preview||previewWasSilent)&&minecraft.level!=null&&minecraft.level.dimension().location().toString().equals("blacksouls:hokoniwa")&&ClientSceneState.getSceneId().equals("map_001");
        if(minecraft.level!=null&&minecraft.player!=null&&minecraft.level.dimension().location().toString().equals("blacksouls:hokoniwa")&&ClientSceneState.getSceneId().equals("map_001")) {
            OriginalNpcPresentationController.apply(new ClientboundOriginalNpcPresentationPacket(223,minecraft.player.getId(),minecraft.level.dimension().location(),
                    List.of(-34.0D,-34.0D,-34.0D,0.0D,0.0D),""));
        }
        ENGINE.clear();stopAmbient();
        OriginalOpeningAmbientController.finish(preview);
        if(capture!=null) { capture.destroyBuffers();capture=null; }
        silentArrival=quiet;
        previewPolicyCaptured=false;previewWasSilent=false;
        if(quiet) { ClientSceneMusic.suspendForPresentation();ownsSceneMusic=true; }
        else if(ownsSceneMusic) { ClientSceneMusic.resumeAfterPresentation();ownsSceneMusic=false; }
        fogRequest.accept(false);
    }
    public static void clear() {
        ENGINE.clear();stopAmbient();
        OriginalOpeningAmbientController.clear();
        if(ownsSceneMusic)ClientSceneMusic.resumeAfterPresentation();
        ownsSceneMusic=false;silentArrival=false;pendingRoomTone=false;previewPolicyCaptured=false;previewWasSilent=false;lastWorldReady=false;fogRequest.accept(false);
        if(capture!=null) { capture.destroyBuffers();capture=null; }
    }

    private static final class Ambient extends AbstractTickableSoundInstance {
        private final float baseVolume;
        private int fadeFrames,fadeTotal;
        private boolean started;
        private Ambient(ResourceLocation id,float volume,float pitch) {
            super(SoundEvent.createVariableRangeEvent(id),SoundSource.AMBIENT,RandomSource.create());
            baseVolume=volume;this.volume=volume;this.pitch=pitch;looping=true;relative=true;attenuation=Attenuation.NONE;
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            if(Minecraft.getInstance().isPaused())return;
            if(fadeFrames>0) { fadeFrames=Math.max(0,fadeFrames-3);volume=baseVolume*fadeFrames/fadeTotal;if(fadeFrames==0)stop(); }
        }
        private void fade(int frames) { if(frames<=0)stop();else { fadeTotal=frames;fadeFrames=frames; } }
        private void restoreGain(float gain,int totalFrames) {
            volume=baseVolume*Math.max(0,Math.min(1,gain));
            if(gain<=0)stop();else if(totalFrames>0) { fadeTotal=totalFrames;fadeFrames=Math.round(totalFrames*gain); }
        }
    }

    @Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent public static void register(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"original_opening_screen"),
                    DefaultVertexFormat.POSITION_TEX),shader->screenShader=shader);
        }
    }
}
