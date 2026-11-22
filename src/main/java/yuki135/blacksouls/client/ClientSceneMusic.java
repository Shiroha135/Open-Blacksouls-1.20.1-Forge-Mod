package yuki135.blacksouls.client;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.util.OriginalMapSceneRegistry;
import yuki135.blacksouls.util.HokoniwaDestination;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientSceneMusic {
    private static final String MUSIC_MOD_ID = "blacksouls2music";
    private static final float SCENE_VOLUME_MULTIPLIER = 1.2F;
    private static final ResourceLocation RABBIT_HOLE_AMBIENCE = ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID, "rabbit_hole_low_sen");
    private static String activeSceneId = "";
    private static OriginalMapSceneRegistry.Entry activeEntry;
    private static SimpleSoundInstance activeMusic;
    private static SimpleSoundInstance dialogueMusic;
    private static boolean dialogueMusicActive;
    private static boolean presentationMusicActive;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ClientSceneState.clear();
            reset();
            return;
        }
        if (!Level.OVERWORLD.equals(minecraft.level.dimension()) && !HokoniwaDestination.isHokoniwa(minecraft.level.dimension())) {
            ClientSceneState.clear();
            reset();
            return;
        }
        if (minecraft.player == null || !minecraft.player.isAlive()) {
            stopForDeath();
            return;
        }
        String sceneId = ClientSceneState.getSceneId();
        if (!activeSceneId.equals(sceneId)) {
            changeScene(sceneId);
        }
        if (dialogueMusicActive || presentationMusicActive) {
            return;
        }
        if (activeEntry == null || activeEntry.isSilent() || TurnBattleAudioGate.isActive() || !isAvailable()) {
            return;
        }
        boolean lowSen = switch (activeSceneId) {
            case "map_051", "map_052", "map_053", "map_054" ->
                    minecraft.player.getCapability(BSPlayerStats.CAPABILITY).map(stats -> stats.sen <= 70).orElse(false);
            default -> false;
        };
        ResourceLocation sound = lowSen ? RABBIT_HOLE_AMBIENCE : activeEntry.soundEvent();
        if (activeMusic != null && !activeMusic.getLocation().equals(sound)) {
            stopMusic();
        }
        if (activeMusic == null || !minecraft.getSoundManager().isActive(activeMusic)) {
            startMusic(minecraft, sound, lowSen);
        }
    }

    public static boolean shouldReplaceVanillaMusic() {
        OriginalMapSceneRegistry.Entry entry = OriginalMapSceneRegistry.get(ClientSceneState.getSceneId());
        return entry != null && (entry.isSilent() || isAvailable());
    }

    public static boolean isSceneSound(ResourceLocation soundEvent) {
        return OriginalMapSceneRegistry.isSceneSound(soundEvent);
    }

    public static void reset() {
        presentationMusicActive = false;
        stopDialogueMusic();
        stopMusic();
        activeSceneId = "";
        activeEntry = null;
    }

    public static void stopForDeath() {
        ClientSceneState.clear();
        reset();
    }

    public static void suspendForPresentation() {
        stopMusic();
        presentationMusicActive = true;
    }

    public static void resumeAfterPresentation() {
        presentationMusicActive = false;
    }

    public static void startCheshireDialogue() {
        Minecraft minecraft = Minecraft.getInstance();
        stopDialogueMusic();
        stopMusic();
        dialogueMusicActive = true;
        dialogueMusic = new SimpleSoundInstance(
                ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID, "cheshire_theme"),
                SoundSource.MUSIC, 1.0F, 1.0F, RandomSource.create(), true, 0,
                SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true
        );
        minecraft.getSoundManager().play(dialogueMusic);
    }

    public static void stopDialogueMusic() {
        Minecraft minecraft = Minecraft.getInstance();
        if (dialogueMusic != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().stop(dialogueMusic);
        }
        dialogueMusic = null;
        dialogueMusicActive = false;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSceneState.clear();
        reset();
    }

    private static void changeScene(String sceneId) {
        stopMusic();
        activeSceneId = sceneId == null ? "" : sceneId;
        activeEntry = OriginalMapSceneRegistry.get(activeSceneId);
    }

    private static void startMusic(Minecraft minecraft, ResourceLocation sound, boolean lowSen) {
        activeMusic = new SimpleSoundInstance(
                sound, lowSen ? SoundSource.AMBIENT : SoundSource.MUSIC,
                lowSen ? 1.0F : activeEntry.volume() * SCENE_VOLUME_MULTIPLIER, lowSen ? 1.0F : activeEntry.pitch(),
                RandomSource.create(), true, 0,
                SoundInstance.Attenuation.NONE,
                0.0D, 0.0D, 0.0D, true
        );
        minecraft.getSoundManager().play(activeMusic);
    }

    private static void stopMusic() {
        Minecraft minecraft = Minecraft.getInstance();
        if (activeMusic != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().stop(activeMusic);
        }
        activeMusic = null;
    }

    private static boolean isAvailable() {
        return ModList.get().isLoaded(MUSIC_MOD_ID);
    }

    private ClientSceneMusic() {
    }
}
