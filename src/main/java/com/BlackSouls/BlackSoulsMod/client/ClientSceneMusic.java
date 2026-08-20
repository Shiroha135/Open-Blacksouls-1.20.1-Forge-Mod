package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.util.OriginalMapSceneRegistry;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
    private static String activeSceneId = "";
    private static OriginalMapSceneRegistry.Entry activeEntry;
    private static SimpleSoundInstance activeMusic;
    private static SimpleSoundInstance dialogueMusic;
    private static boolean dialogueMusicActive;

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
        ResourceLocation dimension = minecraft.level.dimension().location();
        if (!HokoniwaDestination.ID.equals(dimension) && !HokoniwaDestination.LEGACY_ID.equals(dimension)) {
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
        if (dialogueMusicActive) {
            return;
        }
        if (activeEntry == null || TurnBattleAudioGate.isActive() || !isAvailable()) {
            return;
        }
        if (activeMusic == null || !minecraft.getSoundManager().isActive(activeMusic)) {
            startMusic(minecraft);
        }
    }

    public static boolean hasAssignedMusic() {
        return OriginalMapSceneRegistry.get(ClientSceneState.getSceneId()) != null && isAvailable();
    }

    public static boolean isSceneSound(ResourceLocation soundEvent) {
        return OriginalMapSceneRegistry.isSceneSound(soundEvent);
    }

    public static void reset() {
        stopDialogueMusic();
        stopMusic();
        activeSceneId = "";
        activeEntry = null;
    }

    public static void stopForDeath() {
        ClientSceneState.clear();
        reset();
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

    private static void startMusic(Minecraft minecraft) {
        activeMusic = new SimpleSoundInstance(
                activeEntry.soundEvent(), SoundSource.MUSIC,
                activeEntry.volume() * SCENE_VOLUME_MULTIPLIER, activeEntry.pitch(),
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
