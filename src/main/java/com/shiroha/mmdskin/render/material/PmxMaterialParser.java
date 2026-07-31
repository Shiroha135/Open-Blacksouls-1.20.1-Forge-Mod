package com.shiroha.mmdskin.render.material;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PmxMaterialParser {
    private static final int MAX_COUNT = 10_000_000;

    public record Material(
            String name,
            float diffuseR,
            float diffuseG,
            float diffuseB,
            float diffuseA,
            float specularR,
            float specularG,
            float specularB,
            float specularStrength,
            float ambientR,
            float ambientG,
            float ambientB,
            int drawFlags,
            float edgeR,
            float edgeG,
            float edgeB,
            float edgeA,
            float edgeScale,
            String texturePath,
            String sphereTexturePath,
            int sphereMode,
            String toonTexturePath
    ) {
    }

    public static List<Material> read(Path path) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(path)).order(ByteOrder.LITTLE_ENDIAN);
        try {
            require(buffer.get() == 'P' && buffer.get() == 'M' && buffer.get() == 'X' && buffer.get() == ' ', "Invalid PMX magic");
            float version = buffer.getFloat();
            require(version >= 1.99F && version <= 2.11F, "Unsupported PMX version: " + version);

            int headerSize = Byte.toUnsignedInt(buffer.get());
            require(headerSize >= 8 && headerSize <= buffer.remaining(), "Invalid PMX header size: " + headerSize);
            byte[] header = new byte[headerSize];
            buffer.get(header);

            Charset charset = header[0] == 0 ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_8;
            int additionalUvCount = Byte.toUnsignedInt(header[1]);
            int vertexIndexSize = checkedIndexSize(header[2]);
            int textureIndexSize = checkedIndexSize(header[3]);
            int boneIndexSize = checkedIndexSize(header[5]);

            for (int i = 0; i < 4; i++) {
                readText(buffer, charset);
            }

            int vertexCount = readCount(buffer, "vertex");
            for (int i = 0; i < vertexCount; i++) {
                skip(buffer, 32L + (long) additionalUvCount * 16L);
                int deform = Byte.toUnsignedInt(buffer.get());
                long deformSize = switch (deform) {
                    case 0 -> boneIndexSize;
                    case 1 -> boneIndexSize * 2L + 4L;
                    case 2, 4 -> boneIndexSize * 4L + 16L;
                    case 3 -> boneIndexSize * 2L + 40L;
                    default -> throw new IOException("Unsupported PMX deform type: " + deform);
                };
                skip(buffer, deformSize + 4L);
            }

            int surfaceIndexCount = readCount(buffer, "surface index");
            skip(buffer, (long) surfaceIndexCount * vertexIndexSize);

            int textureCount = readCount(buffer, "texture");
            List<String> textures = new ArrayList<>(textureCount);
            for (int i = 0; i < textureCount; i++) {
                textures.add(readText(buffer, charset));
            }

            int materialCount = readCount(buffer, "material");
            List<Material> materials = new ArrayList<>(materialCount);
            for (int i = 0; i < materialCount; i++) {
                String name = readText(buffer, charset);
                readText(buffer, charset);

                float diffuseR = buffer.getFloat();
                float diffuseG = buffer.getFloat();
                float diffuseB = buffer.getFloat();
                float diffuseA = buffer.getFloat();
                float specularR = buffer.getFloat();
                float specularG = buffer.getFloat();
                float specularB = buffer.getFloat();
                float specularStrength = buffer.getFloat();
                float ambientR = buffer.getFloat();
                float ambientG = buffer.getFloat();
                float ambientB = buffer.getFloat();
                int drawFlags = Byte.toUnsignedInt(buffer.get());
                float edgeR = buffer.getFloat();
                float edgeG = buffer.getFloat();
                float edgeB = buffer.getFloat();
                float edgeA = buffer.getFloat();
                float edgeScale = buffer.getFloat();
                int textureIndex = readIndex(buffer, textureIndexSize);
                int sphereTextureIndex = readIndex(buffer, textureIndexSize);
                int sphereMode = Byte.toUnsignedInt(buffer.get());
                int toonSharing = Byte.toUnsignedInt(buffer.get());
                int toonIndex = toonSharing == 0
                        ? readIndex(buffer, textureIndexSize)
                        : Byte.toUnsignedInt(buffer.get());
                readText(buffer, charset);
                readCount(buffer, "material surface index");

                materials.add(new Material(
                        name,
                        diffuseR,
                        diffuseG,
                        diffuseB,
                        diffuseA,
                        specularR,
                        specularG,
                        specularB,
                        specularStrength,
                        ambientR,
                        ambientG,
                        ambientB,
                        drawFlags,
                        edgeR,
                        edgeG,
                        edgeB,
                        edgeA,
                        edgeScale,
                        textureAt(textures, textureIndex),
                        textureAt(textures, sphereTextureIndex),
                        sphereMode,
                        toonSharing == 0 ? textureAt(textures, toonIndex) : ""
                ));
            }
            return materials;
        } catch (BufferUnderflowException | IndexOutOfBoundsException exception) {
            throw new IOException("Truncated PMX material data: " + path, exception);
        }
    }

    private static int checkedIndexSize(byte value) throws IOException {
        int size = Byte.toUnsignedInt(value);
        require(size == 1 || size == 2 || size == 4, "Invalid PMX index size: " + size);
        return size;
    }

    private static int readIndex(ByteBuffer buffer, int size) {
        return switch (size) {
            case 1 -> buffer.get();
            case 2 -> buffer.getShort();
            default -> buffer.getInt();
        };
    }

    private static int readCount(ByteBuffer buffer, String label) throws IOException {
        int count = buffer.getInt();
        require(count >= 0 && count <= MAX_COUNT, "Invalid PMX " + label + " count: " + count);
        return count;
    }

    private static String readText(ByteBuffer buffer, Charset charset) throws IOException {
        int length = buffer.getInt();
        require(length >= 0 && length <= buffer.remaining(), "Invalid PMX text length: " + length);
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, charset);
    }

    private static String textureAt(List<String> textures, int index) {
        return index >= 0 && index < textures.size() ? textures.get(index) : "";
    }

    private static void skip(ByteBuffer buffer, long amount) throws IOException {
        require(amount >= 0L && amount <= buffer.remaining(), "PMX section exceeds file bounds");
        buffer.position(buffer.position() + (int) amount);
    }

    private static void require(boolean condition, String message) throws IOException {
        if (!condition) {
            throw new IOException(message);
        }
    }

    private PmxMaterialParser() {
    }
}
