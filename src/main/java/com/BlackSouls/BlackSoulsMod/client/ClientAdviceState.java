package com.BlackSouls.BlackSoulsMod.client;

public final class ClientAdviceState {
    private static boolean controlled;
    private static boolean visible = true;

    public static void set(boolean isControlled, boolean isVisible) {
        controlled = isControlled;
        visible = !controlled || isVisible;
    }

    public static boolean isVisible() {
        return !controlled || visible;
    }

    private ClientAdviceState() {
    }
}
