package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientSoundEventHandler {

    private static int lastScreenIdentity = 0;
    private static boolean lastScreenWasRPGUI = false;

    private static boolean wasInWorld = false;

    private static int logoutSoundDelay = 0;
    private static int closeSoundDelay = 0;

    @SubscribeEvent
    public static void onScreenTransition(ScreenEvent.Opening event) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getSoundManager() == null) return;

            Screen newScreen = event.getNewScreen();
            Screen oldScreen = event.getCurrentScreen();

            if (oldScreen instanceof SelectWorldScreen && newScreen != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.MAGIC1_EVENT.get(), 1.0F, 1.0F));
            }
        } catch (Exception e) {}
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getSoundManager() == null) return;

            Screen currentScreen = mc.screen;
            boolean isInWorld = (mc.level != null);

            if (wasInWorld && !isInWorld) {
                logoutSoundDelay = 15;
            }
            wasInWorld = isInWorld; 
            if (logoutSoundDelay > 0) {
                boolean isMainMenu = currentScreen instanceof TitleScreen
                        || (currentScreen != null && currentScreen.getClass().getName().contains("GuiBSMainMenu"));

                if (isMainMenu) {
                    logoutSoundDelay--;
                    if (logoutSoundDelay == 0) {
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.MBJH_ME03_EVENT.get(), 1.0F, 1.0F));
                    }
                }
            }

            if (closeSoundDelay > 0) {
                closeSoundDelay--;
                if (closeSoundDelay == 0) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.SWORD3_EVENT.get(), 1.0F, 1.0F));
                }
            }

            int currentScreenIdentity = currentScreen == null ? 0 : System.identityHashCode(currentScreen);
            if (lastScreenIdentity != currentScreenIdentity) {
                boolean wasRPGUI = lastScreenWasRPGUI;
                boolean isRPGUI = isRPGUIScreen(currentScreen);

                if (isRPGUI) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(BlackSouls.SWORD1_EVENT.get(), 1.0F, 1.0F));
                }

                if (wasRPGUI && currentScreen == null) {
                    closeSoundDelay = 3;
                }

                lastScreenIdentity = currentScreenIdentity;
                lastScreenWasRPGUI = isRPGUI;
            }
        } catch (Exception e) {}
    }

    private static boolean isRPGUIScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        String className = screen.getClass().getName();
        return className.startsWith("com.BlackSouls.BlackSoulsMod.client.gui")
                && !className.contains("GuiBSMainMenu");
    }
}
