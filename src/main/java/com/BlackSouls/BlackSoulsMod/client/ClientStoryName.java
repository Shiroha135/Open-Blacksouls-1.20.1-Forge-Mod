package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiStoryNameConfirm;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiStoryNameInput;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiStoryNameIntro;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class ClientStoryName {
    private static String storyName = "";
    private static String pendingName = "";
    private static boolean pendingOpening;
    private static SimpleSoundInstance ambience;

    public static void requestOpening(String candidate) {
        pendingName = candidate == null ? "" : candidate;
        pendingOpening = true;
        tryOpen();
    }

    public static void accept(String name) {
        storyName = name == null ? "" : name;
        pendingOpening = false;
    }

    public static String get(Player player) {
        return storyName.isEmpty() ? player.getName().getString() : storyName;
    }

    public static void finishLocally(String name) {
        storyName = name;
        pendingOpening = false;
        stopAmbience();
    }

    public static void playCursor() {
        if (BlackSouls.CURSOR1_EVENT.isPresent()) {
            Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(BlackSouls.CURSOR1_EVENT.get(), 1.0F, 1.0F)
            );
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            pendingOpening = false;
            storyName = "";
            stopAmbience();
            return;
        }
        if (pendingOpening) {
            tryOpen();
        }
    }

    private static void tryOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!pendingOpening
            || minecraft.player == null
            || minecraft.level == null
            || minecraft.screen != null) {
            return;
        }
        pendingOpening = false;
        startAmbience();
        minecraft.setScreen(new GuiStoryNameIntro(pendingName));
    }

    private static void startAmbience() {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        if (ambience != null) {
            soundManager.stop(ambience);
        }
        ambience = new SimpleSoundInstance(
            BlackSouls.STORY_NAME_SEA_EVENT.get().getLocation(),
            SoundSource.AMBIENT,
            0.5F,
            0.85F,
            RandomSource.create(),
            true,
            0,
            net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,
            0.0D,
            0.0D,
            0.0D,
            true
        );
        soundManager.play(ambience);
    }

    private static void stopAmbience() {
        if (ambience == null) {
            return;
        }
        Minecraft.getInstance().getSoundManager().stop(ambience);
        ambience = null;
    }

    private ClientStoryName() {
    }
}
