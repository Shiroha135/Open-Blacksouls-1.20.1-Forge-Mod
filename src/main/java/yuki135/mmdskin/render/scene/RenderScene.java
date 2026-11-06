package yuki135.mmdskin.render.scene;

public final class RenderScene {
    public static final RenderScene WORLD = new RenderScene();
    private RenderScene() { }
    public boolean isWorldScene() { return true; }
}
