package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TurnBattleAudioGate {
    private static final ThreadLocal<Integer> BATTLE_SOUND_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static Screen battleScreen;

    public static void enter(Screen screen) {
        if (battleScreen == screen) {
            return;
        }
        battleScreen = screen;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().stop();
        }
    }

    public static void leave(Screen screen) {
        if (battleScreen == screen) {
            battleScreen = null;
            BATTLE_SOUND_DEPTH.remove();
        }
    }

    public static void play(SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (sound == null || minecraft.getSoundManager() == null) {
            return;
        }
        BATTLE_SOUND_DEPTH.set(BATTLE_SOUND_DEPTH.get() + 1);
        try {
            minecraft.getSoundManager().play(sound);
        } finally {
            int depth = BATTLE_SOUND_DEPTH.get() - 1;
            if (depth <= 0) {
                BATTLE_SOUND_DEPTH.remove();
            } else {
                BATTLE_SOUND_DEPTH.set(depth);
            }
        }
    }

    public static boolean isActive() {
        return battleScreen != null;
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (battleScreen != null && BATTLE_SOUND_DEPTH.get() <= 0) {
            event.setSound(null);
        }
    }

    private TurnBattleAudioGate() {
    }
}
