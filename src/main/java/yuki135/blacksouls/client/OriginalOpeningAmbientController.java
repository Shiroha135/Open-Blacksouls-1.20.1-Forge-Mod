package yuki135.blacksouls.client;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT)
public final class OriginalOpeningAmbientController {
    private static final OriginalOpeningAmbientSnapshot LEDGER=new OriginalOpeningAmbientSnapshot();
    private static final Map<SoundInstance,Observed> OBSERVED=new IdentityHashMap<>();
    private static long identity;
    private static List<Sound> capturedFiles;
    private OriginalOpeningAmbientController() {
    }
    public static synchronized void capturePreview() {
        if(capturedFiles!=null)return;
        update(false);
        var streams=new java.util.TreeMap<Long,Sound>();
        for(var entry:OBSERVED.entrySet())if(entry.getValue().started&&entry.getValue().streaming)streams.put(entry.getValue().id,entry.getKey().getSound());
        capturedFiles=new ArrayList<>();
        for(var entry:LEDGER.entries().entrySet())if(streams.containsKey(entry.getKey()))capturedFiles.add(streams.get(entry.getKey()));
        LEDGER.capturePreview(streams.keySet());
    }
    public static synchronized void begin(boolean preview) {
        if(LEDGER.managed())return;
        LEDGER.begin(preview);if(LEDGER.suspended())stopObserved();
    }
    public static synchronized void stopSourceBgs() { LEDGER.stopSource();stopObserved(); }
    public static synchronized void finish(boolean replay) {
        stopObserved();
        var restore=LEDGER.finish(replay);OBSERVED.clear();
        var minecraft=Minecraft.getInstance();
        for(int index=0;index<restore.size();index++) {
            var loop=restore.get(index);
            var sound=new Resumed(loop,capturedFiles==null||index>=capturedFiles.size()?null:capturedFiles.get(index));observe(sound,false);
            minecraft.getSoundManager().play(sound);
        }
        capturedFiles=null;
    }
    public static synchronized void clear() { stopObserved();OBSERVED.clear();LEDGER.clear();capturedFiles=null;identity=0; }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static synchronized void play(PlaySoundEvent event) {
        var sound=event.getSound();
        if(sound==null||sound.getSource()!=SoundSource.AMBIENT||!sound.isLooping()||sound instanceof Resumed||sound.getLocation().getPath().equals("original_opening_gear"))return;
        observe(sound,false);
        if(LEDGER.suspended())event.setSound(null);
    }
    @SubscribeEvent public static synchronized void streaming(PlayStreamingSourceEvent event) {
        var observed=OBSERVED.get(event.getSound());if(observed!=null) { observed.started=true;observed.streaming=true; }
    }
    @SubscribeEvent public static synchronized void buffered(PlaySoundSourceEvent event) {
        var observed=OBSERVED.remove(event.getSound());if(observed!=null)LEDGER.remove(observed.id);
    }
    private static void observe(SoundInstance sound,boolean started) {
        if(OBSERVED.containsKey(sound))return;
        long id=++identity;
        var descriptor=sound instanceof Resumed resumed?resumed.captured:descriptor(sound,0,false);
        if(LEDGER.observe(id,descriptor))OBSERVED.put(sound,new Observed(id,started));
    }
    private static OriginalOpeningAmbientSnapshot.Loop descriptor(SoundInstance sound,double seconds,boolean limited) {
        return new OriginalOpeningAmbientSnapshot.Loop(sound.getLocation().toString(),Math.max(0,Math.min(16,sound.getVolume())),Math.max(.01F,Math.min(4,sound.getPitch())),
                sound.getX(),sound.getY(),sound.getZ(),sound.isRelative(),sound.getAttenuation().name(),seconds,limited);
    }
    @SubscribeEvent public static synchronized void tick(TickEvent.ClientTickEvent event) { if(event.phase==TickEvent.Phase.END)update(true); }
    private static void update(boolean advance) {
        var minecraft=Minecraft.getInstance();
        boolean paused=minecraft.isPaused();
        for(Iterator<Map.Entry<SoundInstance,Observed>> iterator=OBSERVED.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();var sound=entry.getKey();var value=entry.getValue();
            boolean active=minecraft.getSoundManager().isActive(sound);
            if(!LEDGER.suspended()&&value.started&&!active) { LEDGER.remove(value.id);iterator.remove();continue; }
            LEDGER.update(value.id,descriptor(sound,0,false),advance&&active&&value.started,paused);
        }
    }
    private static void stopObserved() { var manager=Minecraft.getInstance().getSoundManager();for(var sound:OBSERVED.keySet())manager.stop(sound); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    private static final class Observed {
        final long id;boolean started,streaming;
        Observed(long id,boolean started) { this.id=id;this.started=started; }
    }
    private static final class Resumed extends AbstractTickableSoundInstance {
        private final OriginalOpeningAmbientSnapshot.Loop captured;
        private final Sound capturedFile;
        Resumed(OriginalOpeningAmbientSnapshot.Loop captured,Sound capturedFile) {
            super(SoundEvent.createVariableRangeEvent(ResourceLocation.tryParse(captured.sound())),SoundSource.AMBIENT,RandomSource.create());
            this.captured=captured;this.capturedFile=capturedFile;volume=captured.volume();pitch=captured.pitch();x=captured.x();y=captured.y();z=captured.z();
            relative=captured.relative();attenuation=Attenuation.valueOf(captured.attenuation());looping=true;
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public net.minecraft.client.sounds.WeighedSoundEvents resolve(net.minecraft.client.sounds.SoundManager manager) {
            var event=super.resolve(manager);
            var value=capturedFile==null?this.sound:capturedFile;
            this.sound=new Sound(value.getLocation().toString(),value.getVolume(),value.getPitch(),
                    value.getWeight(),value.getType(),true,value.shouldPreload(),value.getAttenuationDistance());
            return event;
        }
        @Override public void tick() {
        }
        @Override public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library,Sound sound,boolean looping) {
            return library.getStream(sound.getPath(),true).thenApply(stream->{
                try { return OriginalNpcMusicController.seekForward(stream,captured.limited()?0:captured.seconds()); }
                catch(java.io.IOException failure) {
                    try { stream.close(); }catch(java.io.IOException close) { failure.addSuppressed(close); }
                    throw new CompletionException(failure);
                }
            });
        }
    }
}
