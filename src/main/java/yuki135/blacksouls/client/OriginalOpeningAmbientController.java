package yuki135.blacksouls.client;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
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
    private static final Set<SoundInstance> PENDING=Collections.newSetFromMap(new IdentityHashMap<>());
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
    public static void finish(boolean replay) {
        var restore=new ArrayList<Resumed>();
        synchronized(OriginalOpeningAmbientController.class) {
            stopObserved();
            var loops=LEDGER.finish(replay);OBSERVED.clear();
            for(int index=0;index<loops.size();index++) {
                restore.add(new Resumed(loops.get(index),capturedFiles==null||index>=capturedFiles.size()?null:capturedFiles.get(index)));
            }
            capturedFiles=null;
        }
        var manager=Minecraft.getInstance().getSoundManager();
        for(var sound:restore)manager.play(sound);
    }
    public static synchronized void clear() { stopObserved();OBSERVED.clear();LEDGER.clear();capturedFiles=null;identity=0; }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static synchronized void play(PlaySoundEvent event) {
        var sound=event.getSound();
        if(!tracked(sound))return;
        if(LEDGER.suspended())event.setSound(null);
        else PENDING.add(sound);
    }
    @SubscribeEvent public static synchronized void streaming(PlayStreamingSourceEvent event) {
        var sound=event.getSound();
        if(!tracked(sound))return;
        boolean pending=PENDING.remove(sound);
        if(!pending||LEDGER.suspended()) { event.getChannel().stop();return; }
        observe(sound,true);
        var observed=OBSERVED.get(sound);if(observed!=null) { observed.started=true;observed.streaming=true; }
    }
    @SubscribeEvent public static synchronized void buffered(PlaySoundSourceEvent event) {
        var sound=event.getSound();
        if(tracked(sound)&&(!PENDING.remove(sound)||LEDGER.suspended()))event.getChannel().stop();
        var observed=OBSERVED.remove(sound);if(observed!=null)LEDGER.remove(observed.id);
    }
    private static boolean tracked(SoundInstance sound) {
        return sound!=null&&sound.getSource()==SoundSource.AMBIENT&&sound.isLooping()&&!sound.getLocation().getPath().equals("original_opening_gear");
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
        PENDING.removeIf(sound->!minecraft.getSoundManager().isActive(sound));
        for(Iterator<Map.Entry<SoundInstance,Observed>> iterator=OBSERVED.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();var sound=entry.getKey();var value=entry.getValue();
            boolean active=minecraft.getSoundManager().isActive(sound);
            if(!LEDGER.suspended()&&value.started&&!active) { LEDGER.remove(value.id);iterator.remove();continue; }
            LEDGER.update(value.id,descriptor(sound,0,false),advance&&active&&value.started,paused);
        }
    }
    private static void stopObserved() {
        var manager=Minecraft.getInstance().getSoundManager();
        for(var sound:OBSERVED.keySet())manager.stop(sound);
        for(var sound:PENDING)manager.stop(sound);
        PENDING.clear();
    }
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
        @Override public float getVolume() { return volume; }
        @Override public float getPitch() { return pitch; }
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
