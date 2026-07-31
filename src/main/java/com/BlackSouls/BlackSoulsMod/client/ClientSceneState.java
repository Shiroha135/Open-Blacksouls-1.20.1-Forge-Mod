package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.util.OriginalMapNameRegistry;

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

    private ClientSceneState() {
    }
}
