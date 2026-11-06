package yuki135.blacksouls.client;

import yuki135.blacksouls.BlackSouls;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalNpcMusicController {
    private static final Map<String, Double> DURATIONS = loadDurations();
    private static Music current;
    private static Track saved;
    private static boolean hasSaved;
    private static volatile boolean ownsMusic;
    private static String sceneStamp = "";
    private static volatile ObservedMusic observed;

    private OriginalNpcMusicController() {
    }

    public static void apply(int code, String sound, List<Double> values) {
        switch (code) {
            case 241 -> play(sound.isEmpty() ? null : new Track(java.util.Objects.requireNonNull(ResourceLocation.tryParse(sound)),
                    values.get(0).floatValue() / 100.0F, values.get(1).floatValue() / 100.0F, 0.0D, true), true);
            case 242 -> {
                if (!ownsMusic) {
                    play(snapshot(), false);
                }
                if (current != null) {
                    current.fade(values.get(0).intValue() * 20);
                }
            }
            case 243 -> {
                saved = snapshot();
                hasSaved = true;
            }
            case 244 -> {
                if (hasSaved) {
                    play(saved, false);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported original music command " + code);
        }
    }

    private static Track snapshot() {
        if (ownsMusic) {
            return current == null || current.isStopped() ? null : current.snapshot();
        }
        ObservedMusic music = observed;
        return music == null ? null : music.snapshot();
    }

    private static void play(Track track, boolean keepSameTrackPosition) {
        Minecraft minecraft = Minecraft.getInstance();
        if (keepSameTrackPosition && current != null && !current.isStopped() && track != null
                && current.track.sound().equals(track.sound()) && current.track.looping() == track.looping()) {
            current.changeParameters(track);
            return;
        }
        if (!ownsMusic) {
            sceneStamp = ClientSceneState.getSceneId();
            ownsMusic = true;
            ClientSceneMusic.suspendForPresentation();
            minecraft.getSoundManager().stop(null, SoundSource.MUSIC);
        }
        if (current != null) {
            minecraft.getSoundManager().stop(current);
        }
        current = track == null ? null : new Music(track);
        if (current != null) {
            minecraft.getSoundManager().play(current);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !minecraft.player.isAlive()) {
            reset();
            return;
        }
        if (ownsMusic && !sceneStamp.equals(ClientSceneState.getSceneId())) {
            Track memory = saved;
            boolean hadMemory = hasSaved;
            reset();
            saved = memory;
            hasSaved = hadMemory;
        }
        if (minecraft.isPaused() || TurnBattleAudioGate.isActive()) {
            return;
        }
        ObservedMusic music = observed;
        if (!ownsMusic && music != null && music.started) {
            music.position += 0.05D * music.sound.getPitch();
        }
        if (ownsMusic && current != null && !current.isStopped()
                && !minecraft.getSoundManager().isActive(current) && current.started) {
            if (minecraft.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F
                    || minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= 0.0F) {
                current.tick();
                return;
            }
            Track resumed = current.snapshot();
            current = new Music(resumed);
            minecraft.getSoundManager().play(current);
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (ownsMusic && !TurnBattleAudioGate.isActive() && sound != null
                && sound.getSource() == SoundSource.MUSIC && !(sound instanceof Music)) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onStreamingSource(PlayStreamingSourceEvent event) {
        SoundInstance sound = event.getSound();
        if (sound instanceof Music music) {
            music.started = true;
        } else if (!ownsMusic && sound.getSource() == SoundSource.MUSIC && !TurnBattleAudioGate.isActive()
                && (DURATIONS.containsKey(sound.getLocation().toString()) || ClientSceneMusic.isSceneSound(sound.getLocation()))) {
            observed = new ObservedMusic(sound);
        }
    }

    public static void reset() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
        }
        current = null;
        saved = null;
        hasSaved = false;
        ownsMusic = false;
        observed = null;
        sceneStamp = "";
        ClientSceneMusic.resumeAfterPresentation();
    }

    public static AudioStream seekForward(AudioStream stream, double seconds) throws IOException {
        AudioFormat format = stream.getFormat();
        if (!Double.isFinite(seconds) || format.getSampleRate() <= 0.0F || format.getFrameSize() <= 0) {
            throw new IOException("Invalid original music playback position or PCM format");
        }
        long remaining = (long) Math.floor(Math.max(0.0D, seconds) * format.getSampleRate()) * format.getFrameSize();
        while (remaining > 0L) {
            ByteBuffer buffer = stream.read((int) Math.min(65536L, remaining));
            if (buffer == null || !buffer.hasRemaining()) {
                break;
            }
            if (buffer.remaining() > remaining) {
                buffer.position(buffer.position() + (int) remaining);
                return new PrefixStream(stream, buffer.slice());
            }
            remaining -= buffer.remaining();
        }
        return stream;
    }

    private static Map<String, Double> loadDurations() {
        try (InputStream stream = OriginalNpcMusicController.class.getResourceAsStream(
                "/data/blacksouls/original_npc_audio.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing original NPC audio durations");
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Double> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> value : root.getAsJsonObject("bgm").entrySet()) {
                JsonObject track = value.getValue().getAsJsonObject();
                result.put(track.get("sound").getAsString(), track.get("durationSeconds").getAsDouble());
            }
            return Map.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original NPC audio durations", exception);
        }
    }

    private record Track(ResourceLocation sound, float volume, float pitch, double position, boolean looping) {
    }

    private static final class ObservedMusic {
        private final SoundInstance sound;
        private volatile double position;
        private final boolean started = true;

        private ObservedMusic(SoundInstance sound) {
            this.sound = sound;
        }

        private Track snapshot() {
            return new Track(sound.getLocation(), sound.getVolume(), sound.getPitch(), position, sound.isLooping());
        }
    }

    private static final class Music extends AbstractTickableSoundInstance {
        private Track track;
        private double position;
        private volatile boolean started;
        private int fadeTicks;
        private int totalFadeTicks;

        private Music(Track track) {
            super(SoundEvent.createVariableRangeEvent(track.sound()), SoundSource.MUSIC, RandomSource.create());
            this.track = track;
            this.volume = track.volume();
            this.pitch = track.pitch();
            this.looping = track.looping();
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.position = track.position();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (started) {
                position += 0.05D * pitch;
            }
            if (fadeTicks > 0) {
                fadeTicks--;
                volume = track.volume() * fadeTicks / totalFadeTicks;
                if (fadeTicks == 0) {
                    stop();
                }
            }
        }

        private void fade(int ticks) {
            if (ticks == 0) {
                stop();
            } else {
                fadeTicks = ticks;
                totalFadeTicks = ticks;
            }
        }

        private void changeParameters(Track replacement) {
            track = new Track(replacement.sound(), replacement.volume(), replacement.pitch(), position, replacement.looping());
            volume = replacement.volume();
            pitch = replacement.pitch();
            fadeTicks = 0;
        }

        private Track snapshot() {
            return new Track(track.sound(), track.volume(), track.pitch(), position, track.looping());
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
            return library.getStream(sound.getPath(), looping).thenApply(stream -> {
                try {
                    double duration = DURATIONS.getOrDefault(track.sound().toString(), 0.0D);
                    double position = looping && duration > 0.0D ? track.position() % duration : track.position();
                    return seekForward(stream, position);
                } catch (IOException exception) {
                    try {
                        stream.close();
                    } catch (IOException closeException) {
                        exception.addSuppressed(closeException);
                    }
                    throw new CompletionException(exception);
                }
            });
        }
    }

    private static final class PrefixStream implements AudioStream {
        private final AudioStream delegate;
        private ByteBuffer prefix;

        private PrefixStream(AudioStream delegate, ByteBuffer prefix) {
            this.delegate = delegate;
            this.prefix = prefix;
        }

        @Override
        public AudioFormat getFormat() {
            return delegate.getFormat();
        }

        @Override
        public ByteBuffer read(int bytes) throws IOException {
            if (prefix != null) {
                ByteBuffer value = prefix;
                prefix = null;
                return value;
            }
            return delegate.read(bytes);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
