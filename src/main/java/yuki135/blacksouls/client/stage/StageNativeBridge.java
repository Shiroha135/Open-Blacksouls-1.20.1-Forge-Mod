package yuki135.blacksouls.client.stage;

public final class StageNativeBridge {
    public static native int GetApiVersion();

    public static native boolean SyncScene(long model, double x, double y, double z,
                                           float yawRadians, float modelScale, float[] relativeBoxes,
                                           int count, boolean reset);

    public static native int GetSceneColliderCount(long model);

    private StageNativeBridge() {
    }
}
