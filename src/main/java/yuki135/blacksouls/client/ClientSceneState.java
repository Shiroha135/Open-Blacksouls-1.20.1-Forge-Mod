package yuki135.blacksouls.client;

import yuki135.blacksouls.util.OriginalMapNameRegistry;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public final class ClientSceneState {
    private static String sceneId = "";
    private static ClientLevel sceneLevel;

    public static void set(String value) {
        sceneId = value == null ? "" : value.trim();
        sceneLevel = sceneId.isEmpty() ? null : Minecraft.getInstance().level;
    }

    public static void clear() {
        sceneId = "";
        sceneLevel = null;
    }

    public static String getDisplayName() {
        return OriginalMapNameRegistry.getDisplayName(getSceneId());
    }

    public static String getSceneId() {
        if (sceneLevel == null || sceneLevel != Minecraft.getInstance().level) {
            clear();
        }
        return sceneId;
    }

    public static ResourceLocation getBattleBackground() {
        String value = getSceneId().toLowerCase(Locale.ROOT);
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
