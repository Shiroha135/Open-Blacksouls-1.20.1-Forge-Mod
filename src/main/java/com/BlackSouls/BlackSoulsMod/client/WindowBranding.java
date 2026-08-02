package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class WindowBranding {

    public static final String TITLE = "BLACK SOULS II -By HatsuYuki135";
    private static final PackResources ICONS = new WindowIconPack();
    private static volatile boolean enabled;

    private WindowBranding() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void apply(boolean useOriginalBranding) {
        enabled = useOriginalBranding;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.updateTitle();

        IconSet iconSet = SharedConstants.getCurrentVersion().isStable() ? IconSet.RELEASE : IconSet.SNAPSHOT;
        try {
            applyIcon(minecraft, useOriginalBranding ? ICONS : minecraft.getVanillaPackResources(), iconSet);
        } catch (IOException exception) {
            BlackSouls.LOGGER.warn("Failed to update the game window icon", exception);
        }
    }

    private static void applyIcon(Minecraft minecraft, PackResources resources, IconSet iconSet) throws IOException {
        List<IoSupplier<InputStream>> suppliers = iconSet.getStandardIcons(resources);
        List<ByteBuffer> pixels = new ArrayList<>(suppliers.size());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer images = GLFWImage.malloc(suppliers.size(), stack);
            for (int index = 0; index < suppliers.size(); index++) {
                try (InputStream stream = suppliers.get(index).get(); NativeImage image = NativeImage.read(stream)) {
                    ByteBuffer buffer = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
                    pixels.add(buffer);
                    buffer.asIntBuffer().put(image.getPixelsRGBA());
                    images.position(index);
                    images.width(image.getWidth());
                    images.height(image.getHeight());
                    images.pixels(buffer);
                }
            }
            GLFW.glfwSetWindowIcon(minecraft.getWindow().getWindow(), images.position(0));
        } finally {
            pixels.forEach(MemoryUtil::memFree);
        }
    }

    private static final class WindowIconPack implements PackResources {

        @Override
        public IoSupplier<InputStream> getRootResource(String... elements) {
            String fileName = elements[elements.length - 1];
            String resource = switch (fileName) {
                case "icon_16x16.png", "icon_32x32.png", "icon_48x48.png", "icon_128x128.png", "icon_256x256.png" ->
                        "/assets/blacksouls/window/" + fileName;
                default -> null;
            };
            if (resource == null) {
                return null;
            }
            return () -> {
                InputStream stream = WindowBranding.class.getResourceAsStream(resource);
                if (stream == null) {
                    throw new FileNotFoundException(resource);
                }
                return stream;
            };
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            return null;
        }

        @Override
        public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return Set.of();
        }

        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
            return null;
        }

        @Override
        public String packId() {
            return "blacksouls_window_icons";
        }

        @Override
        public void close() {
        }
    }
}
