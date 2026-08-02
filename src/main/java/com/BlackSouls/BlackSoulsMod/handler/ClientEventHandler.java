package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiBSMainMenu;
import com.BlackSouls.BlackSoulsMod.client.ClientSceneMusic;
import com.BlackSouls.BlackSoulsMod.client.TurnBattleAudioGate;
import com.BlackSouls.BlackSoulsMod.client.render.GuiShaderTextRenderer;
import com.BlackSouls.BlackSoulsMod.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = BlackSouls.MODID)
public class ClientEventHandler {

    private static SoundInstance currentTitleBGM = null;
    private static SoundInstance currentLibraryBGM = null;
    private static final ResourceLocation LIBRARY_DIM_ID = new ResourceLocation(BlackSouls.MODID, "library");
    private static final ResourceLocation LIBRARY_BGM_ID = new ResourceLocation(BlackSouls.MODID, "music.library");

    private static final String HATSUYUKI_CLIENT_LITE_MODID = "hatsuyukiclientlite";

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof TitleScreen) {
            if (!ModList.get().isLoaded(HATSUYUKI_CLIENT_LITE_MODID)) {
                event.setNewScreen(new GuiBSMainMenu());
            }
            return;
        }

        if (event.getScreen() instanceof net.minecraft.client.gui.screens.DeathScreen) {
            stopWorldMusicForDeath();
            event.setNewScreen(new com.BlackSouls.BlackSoulsMod.client.gui.GuiYouDied());
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }

        Component title = screen.getTitle();
        String plainTitle = net.minecraft.ChatFormatting.stripFormatting(title.getString());
        if (plainTitle == null || !plainTitle.contains("\u5f00\u53d1\u8005\u7269\u54c1")) {
            return;
        }

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        int drawX = accessor.blacksouls$getLeftPos() + accessor.blacksouls$getTitleLabelX();
        int drawY = accessor.blacksouls$getTopPos() + accessor.blacksouls$getTitleLabelY();
        int textWidth = font.width(title);
        int textHeight = font.lineHeight;

        guiGraphics.fill(drawX - 2, drawY - 1, drawX + textWidth + 2, drawY + textHeight, 0xFFC6C6C6);
        GuiShaderTextRenderer.renderRainbowText(guiGraphics, font, title, drawX, drawY);
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() != null && event.getSound().getLocation().getPath().contains("music.menu")) {
            event.setSound(null);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != null && level.dimension().location().equals(LIBRARY_DIM_ID) && event.getSound() != null) {
            ResourceLocation soundId = event.getSound().getLocation();
            if (!LIBRARY_BGM_ID.equals(soundId)
                    && !ClientSceneMusic.isSceneSound(soundId)
                    && soundId.getPath().contains("music")) {
                event.setSound(null);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        ClientLevel level = mc.level;

        boolean inLibrary = level != null && level.dimension().location().equals(LIBRARY_DIM_ID);
        if (inLibrary) {
            mc.getMusicManager().stopPlaying();
            if (currentTitleBGM != null) {
                mc.getSoundManager().stop(currentTitleBGM);
                currentTitleBGM = null;
            }

            if (mc.player == null || !mc.player.isAlive()) {
                stopWorldMusicForDeath();
                return;
            }

            if (ClientSceneMusic.hasAssignedMusic()) {
                if (currentLibraryBGM != null) {
                    mc.getSoundManager().stop(currentLibraryBGM);
                    currentLibraryBGM = null;
                }
                return;
            }
            if (!TurnBattleAudioGate.isActive()
                    && (currentLibraryBGM == null || !mc.getSoundManager().isActive(currentLibraryBGM))) {
                currentLibraryBGM = SimpleSoundInstance.forMusic(BlackSouls.LIBRARY_BGM_EVENT.get());
                mc.getSoundManager().play(currentLibraryBGM);
            }
            return;
        }

        if (currentLibraryBGM != null) {
            mc.getSoundManager().stop(currentLibraryBGM);
            currentLibraryBGM = null;
        }

        if (screen instanceof TitleScreen || screen instanceof GuiBSMainMenu) {
            if (currentTitleBGM == null || !mc.getSoundManager().isActive(currentTitleBGM)) {
                currentTitleBGM = SimpleSoundInstance.forMusic(BlackSouls.TITLE_BGM_EVENT.get());
                mc.getSoundManager().play(currentTitleBGM);
            }
        } else if (currentTitleBGM != null) {
            mc.getSoundManager().stop(currentTitleBGM);
            currentTitleBGM = null;
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && BlackSouls.BUFF_MADNESS.isPresent() && mc.player.hasEffect(BlackSouls.BUFF_MADNESS.get())) {
                mc.player.hurtTime = 0;
            }
        }
    }

    private static void stopWorldMusicForDeath() {
        Minecraft mc = Minecraft.getInstance();
        ClientSceneMusic.stopForDeath();
        mc.getMusicManager().stopPlaying();
        if (currentLibraryBGM != null) {
            mc.getSoundManager().stop(currentLibraryBGM);
            currentLibraryBGM = null;
        }
    }
}
