package com.shiroha.mmdskin.render.material;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL46C;

import java.nio.ByteBuffer;

/**
 * 子网格绘制公共逻辑。
 */
public final class SubMeshDrawHelper {

    private static final int SUB_MESH_STRIDE = 20;

    private SubMeshDrawHelper() {
    }

    @FunctionalInterface
    public interface TextureResolver {
        int resolve(int materialId);
    }

    @FunctionalInterface
    public interface AlphaResolver {
        float resolve(int materialId, float baseAlpha);
    }

    @FunctionalInterface
    public interface DrawFilter {
        boolean test(int materialId, float alpha);
    }

    @FunctionalInterface
    public interface MaterialBinder {
        void bind(int materialId, float alpha);
    }

    public static void draw(ByteBuffer subMeshDataBuf,
                            int subMeshCount,
                            int indexElementSize,
                            int indexType,
                            TextureResolver textureResolver,
                            AlphaResolver alphaResolver) {
        draw(subMeshDataBuf, subMeshCount, indexElementSize, indexType, textureResolver, alphaResolver,
                (materialId, alpha) -> true, (materialId, alpha) -> {
                });
    }

    public static void draw(ByteBuffer subMeshDataBuf,
                            int subMeshCount,
                            int indexElementSize,
                            int indexType,
                            TextureResolver textureResolver,
                            AlphaResolver alphaResolver,
                            DrawFilter drawFilter,
                            MaterialBinder materialBinder) {
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
        int lastBoundTexture = -1;

        for (int i = 0; i < subMeshCount; ++i) {
            int base = i * SUB_MESH_STRIDE;
            int materialId = subMeshDataBuf.getInt(base);
            int beginIndex = subMeshDataBuf.getInt(base + 4);
            int vertexCount = subMeshDataBuf.getInt(base + 8);
            float alpha = subMeshDataBuf.getFloat(base + 12);
            boolean visible = subMeshDataBuf.get(base + 16) != 0;
            boolean bothFace = subMeshDataBuf.get(base + 17) != 0;

            float effectiveAlpha = alphaResolver.resolve(materialId, alpha);
            if (!visible || effectiveAlpha < 0.001f || !drawFilter.test(materialId, effectiveAlpha)) {
                continue;
            }

            if (bothFace) {
                RenderSystem.disableCull();
            } else {
                RenderSystem.enableCull();
            }

            materialBinder.bind(materialId, effectiveAlpha);
            RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
            int textureId = textureResolver.resolve(materialId);
            if (textureId != lastBoundTexture) {
                RenderSystem.setShaderTexture(0, textureId);
                GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, textureId);
                lastBoundTexture = textureId;
            }

            long startPos = (long) beginIndex * indexElementSize;
            GL46C.glDrawElements(GL46C.GL_TRIANGLES, vertexCount, indexType, startPos);
        }
    }

    public static void drawOutline(ByteBuffer subMeshDataBuf,
                                   int subMeshCount,
                                   int indexElementSize,
                                   int indexType,
                                   TextureResolver textureResolver,
                                   AlphaResolver alphaResolver) {
        drawOutline(subMeshDataBuf, subMeshCount, indexElementSize, indexType, textureResolver, alphaResolver,
                (materialId, alpha) -> {
                });
    }

    public static void drawOutline(ByteBuffer subMeshDataBuf,
                                   int subMeshCount,
                                   int indexElementSize,
                                   int indexType,
                                   TextureResolver textureResolver,
                                   AlphaResolver alphaResolver,
                                   MaterialBinder materialBinder) {
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
        int lastBoundTexture = -1;

        for (int i = 0; i < subMeshCount; ++i) {
            int base = i * SUB_MESH_STRIDE;
            int materialId = subMeshDataBuf.getInt(base);
            int beginIndex = subMeshDataBuf.getInt(base + 4);
            int vertexCount = subMeshDataBuf.getInt(base + 8);
            float alpha = subMeshDataBuf.getFloat(base + 12);
            boolean visible = subMeshDataBuf.get(base + 16) != 0;

            float effectiveAlpha = alphaResolver.resolve(materialId, alpha);
            if (!visible || effectiveAlpha < 0.001f) {
                continue;
            }

            materialBinder.bind(materialId, effectiveAlpha);
            RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
            int textureId = textureResolver.resolve(materialId);
            if (textureId != lastBoundTexture) {
                RenderSystem.setShaderTexture(0, textureId);
                GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, textureId);
                lastBoundTexture = textureId;
            }

            long startPos = (long) beginIndex * indexElementSize;
            GL46C.glDrawElements(GL46C.GL_TRIANGLES, vertexCount, indexType, startPos);
        }
    }
}
