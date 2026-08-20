package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.util.OriginalMapNameRegistry;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public final class ClientSceneState {
    private static String sceneId = "";

    public static void set(String value) {
        sceneId = value == null ? "" : value.trim();
    }

    public static void clear() {
        sceneId = "";
    }

    public static String getDisplayName() {
        return OriginalMapNameRegistry.getDisplayName(sceneId);
    }

    public static String getSceneId() {
        return sceneId;
    }

    public static ResourceLocation getBattleBackground() {
        String value = sceneId.toLowerCase(Locale.ROOT);
        if (!value.matches("map_\\d{3}")) {
            return null;
        }
        int mapId = Integer.parseInt(value.substring(4));
        if (mapId < 1 || mapId > 410) {
            return null;
        }
        return new ResourceLocation("blacksouls", "textures/gui/battle/map/" + value + ".png");
    }

    private ClientSceneState() {
    }
}
