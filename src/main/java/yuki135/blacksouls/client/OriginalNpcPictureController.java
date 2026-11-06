package yuki135.blacksouls.client;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.story.OriginalNpcPictureState;
import yuki135.blacksouls.util.OriginalNpcPictures;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalNpcPictureController {
    private static final Scene SCENE = new Scene();
    private static ResourceLocation dimension;
    private static ShaderInstance shader;

    private OriginalNpcPictureController() {
    }

    public static void apply(ResourceLocation packetDimension, long revision, boolean paused,
                              boolean visible, List<JsonObject> pictures) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(packetDimension)) return;
        if (dimension != null && !dimension.equals(packetDimension)) clear();
        if (SCENE.accept(revision, paused, visible, pictures, System.nanoTime())) dimension = packetDimension;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) advance();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && level.isClientSide()
                && (dimension == null || dimension.equals(level.dimension().location()))) clear();
    }

    public static void clear() {
        SCENE.clear();
        dimension = null;
    }

    private static void advance() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }
        if (dimension == null || !dimension.equals(minecraft.level.dimension().location())) return;
        SCENE.advance(System.nanoTime(), minecraft.isPaused() || TurnBattleAudioGate.isActive());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderGuiEvent.Pre event) {
        advance();
        Minecraft minecraft = Minecraft.getInstance();
        if (shader == null || minecraft.level == null || dimension == null
                || !dimension.equals(minecraft.level.dimension().location()) || !SCENE.visible()
                || TurnBattleAudioGate.isActive()) return;
        List<OriginalNpcPictureState> rendered = SCENE.pictures().stream()
                .filter(picture -> !picture.name().isEmpty() && picture.opacity() > 0.0D).toList();
        if (rendered.isEmpty()) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Canvas canvas = canvas(width, height);
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.flush();
        RenderState previous = new RenderState();
        graphics.enableScissor((int) Math.floor(canvas.x()), (int) Math.floor(canvas.y()),
                (int) Math.ceil(canvas.x() + canvas.width()), (int) Math.ceil(canvas.y() + canvas.height()));
        try {
            previous.limitScissor();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            double[] flash = OriginalNpcPresentationController.pictureFlash();
            shader.safeGetUniform("Flash").set((float) (flash[0] / 255.0D), (float) (flash[1] / 255.0D),
                    (float) (flash[2] / 255.0D), (float) (flash[3] / 255.0D));
            shader.safeGetUniform("Brightness").set(OriginalNpcPresentationController.pictureBrightness() / 255.0F);
            for (OriginalNpcPictureState picture : rendered) {
                OriginalNpcPictures.Entry asset = OriginalNpcPictures.get(picture.name());
                if (asset == null) continue;
                Bounds rectangle = bounds(asset.width(), asset.height(), picture.origin(), picture.x(), picture.y(),
                        picture.zoomX(), picture.zoomY(), width, height);
                ResourceLocation texture = ResourceLocation.tryParse(asset.texture());
                RenderSystem.setShaderTexture(0, texture);
                shader.setSampler("Sampler0", RenderSystem.getShaderTexture(0));
                shader.safeGetUniform("Opacity").set((float) (picture.opacity() / 255.0D));
                BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                vertex(buffer, rectangle.left(), rectangle.bottom(), 0.0F, 1.0F, width, height);
                vertex(buffer, rectangle.right(), rectangle.bottom(), 1.0F, 1.0F, width, height);
                vertex(buffer, rectangle.right(), rectangle.top(), 1.0F, 0.0F, width, height);
                vertex(buffer, rectangle.left(), rectangle.top(), 0.0F, 0.0F, width, height);
                BufferUploader.drawWithShader(buffer.end());
            }
        } finally {
            graphics.disableScissor();
            previous.restore();
        }
    }

    private static void vertex(BufferBuilder buffer, double x, double y, float u, float v, int width, int height) {
        buffer.vertex(x * 2.0D / width - 1.0D, 1.0D - y * 2.0D / height, 0.0D).uv(u, v).endVertex();
    }

    public static Canvas canvas(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid original picture canvas");
        double scale = Math.min(width / 640.0D, height / 480.0D);
        return new Canvas((width - 640.0D * scale) / 2.0D, (height - 480.0D * scale) / 2.0D,
                640.0D * scale, 480.0D * scale, scale);
    }

    public static Bounds bounds(int bitmapWidth, int bitmapHeight, int origin, double x, double y,
                                 double zoomX, double zoomY, int canvasWidth, int canvasHeight) {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || origin < 0 || origin > 1) {
            throw new IllegalArgumentException("Invalid original picture geometry");
        }
        Canvas canvas = canvas(canvasWidth, canvasHeight);
        double originX = origin == 0 ? 0 : bitmapWidth / 2;
        double originY = origin == 0 ? 0 : bitmapHeight / 2;
        double left = canvas.x() + (x - originX * zoomX / 100.0D) * canvas.scale();
        double top = canvas.y() + (y - originY * zoomY / 100.0D) * canvas.scale();
        return new Bounds(left, top, left + bitmapWidth * zoomX / 100.0D * canvas.scale(),
                top + bitmapHeight * zoomY / 100.0D * canvas.scale());
    }

    public record Canvas(double x, double y, double width, double height, double scale) {
    }

    public record Bounds(double left, double top, double right, double bottom) {
    }

    public static final class Scene {
        private final Map<Integer, OriginalNpcPictureState> pictures = new TreeMap<>();
        private long revision = -1L;
        private boolean paused;
        private boolean visible;
        private long lastNanos;
        private long remainder;
        private boolean clockReady;

        public boolean accept(long incomingRevision, boolean paused, boolean visible,
                              List<JsonObject> snapshots, long now) {
            if (incomingRevision < 0L || snapshots == null || snapshots.size() > 100) {
                throw new IllegalArgumentException("Invalid original picture snapshot");
            }
            if (incomingRevision <= revision) return false;
            Map<Integer, OriginalNpcPictureState> replacement = new TreeMap<>();
            for (JsonObject snapshot : snapshots) {
                OriginalNpcPictureState picture = OriginalNpcPictureState.restore(snapshot);
                if (!picture.name().isEmpty() && (OriginalNpcPictures.get(picture.name()) == null || picture.blendType() != 0)) {
                    throw new IllegalArgumentException("Unsupported original picture resource or blend");
                }
                if (replacement.put(picture.number(), picture) != null) {
                    throw new IllegalArgumentException("Duplicate original picture number");
                }
            }
            pictures.clear();
            pictures.putAll(replacement);
            revision = incomingRevision;
            this.paused = paused;
            this.visible = visible;
            lastNanos = now;
            remainder = 0L;
            clockReady = true;
            return true;
        }

        public void advance(long now, boolean locallyPaused) {
            if (!clockReady) return;
            long elapsed = Math.max(0L, now - lastNanos);
            lastNanos = now;
            if (paused || locallyPaused) return;
            long fractional = elapsed % 1_000_000_000L * 60L + remainder;
            long frames = elapsed / 1_000_000_000L * 60L + fractional / 1_000_000_000L;
            remainder = fractional % 1_000_000_000L;
            if (frames <= 0L) return;
            for (OriginalNpcPictureState picture : pictures.values()) {
                picture.advanceFrames((int) Math.min(frames, picture.duration()));
            }
        }

        public List<OriginalNpcPictureState> pictures() {
            return List.copyOf(pictures.values());
        }

        public boolean visible() {
            return visible;
        }

        public long revision() {
            return revision;
        }

        public void clear() {
            pictures.clear();
            revision = -1L;
            paused = false;
            visible = false;
            clockReady = false;
            remainder = 0L;
        }
    }

    private static final class RenderState {
        private final ShaderInstance shader = RenderSystem.getShader();
        private final int texture = RenderSystem.getShaderTexture(0);
        private final float[] color = RenderSystem.getShaderColor().clone();
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final int[] scissorBox = readScissor();
        private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

        private static int[] readScissor() {
            int[] box = new int[4];
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
            return box;
        }

        private void limitScissor() {
            if (!scissor) return;
            int[] current = readScissor();
            int left = Math.max(current[0], scissorBox[0]);
            int bottom = Math.max(current[1], scissorBox[1]);
            int right = Math.min(current[0] + current[2], scissorBox[0] + scissorBox[2]);
            int top = Math.min(current[1] + current[3], scissorBox[1] + scissorBox[3]);
            RenderSystem.enableScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
        }

        private void restore() {
            if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            else RenderSystem.disableScissor();
            if (shader != null) RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            RenderSystem.depthMask(depthMask);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        }
    }

    @Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID, "original_npc_picture"),
                    DefaultVertexFormat.POSITION_TEX), instance -> shader = instance);
        }
    }
}
