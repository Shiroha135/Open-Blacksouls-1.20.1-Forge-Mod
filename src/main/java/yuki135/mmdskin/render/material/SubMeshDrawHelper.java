package yuki135.mmdskin.render.material;

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

            float materialAlpha = EntityRenderOpacity.clamp(alphaResolver.resolve(materialId, alpha));
            float effectiveAlpha = EntityRenderOpacity.apply(materialAlpha);
            if (!visible || materialAlpha < 0.001f || effectiveAlpha <= 0.0f || !drawFilter.test(materialId, effectiveAlpha)) {
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

    public static void drawStandard(ByteBuffer subMeshDataBuf, int subMeshCount, int indexElementSize,
                                    int indexType, TextureResolver textures, AlphaResolver alphaResolver,
                                    int program, float red, float green, float blue) {
        if (!EntityRenderOpacity.isScoped()) {
            draw(subMeshDataBuf, subMeshCount, indexElementSize, indexType, textures, alphaResolver);
            return;
        }
        boolean depthMask = GL46C.glGetBoolean(GL46C.GL_DEPTH_WRITEMASK);
        int colorLocation = GL46C.glGetUniformLocation(program, "ColorModulator");
        float[] previousUniform = new float[4];
        if (colorLocation >= 0) GL46C.glGetUniformfv(program, colorLocation, previousUniform);
        int[] attributes = colorLocation >= 0 ? new int[0]
                : new int[]{GL46C.glGetAttribLocation(program, "Color"), GL46C.glGetAttribLocation(program, "iris_Color")};
        float[][] previousAttributes = new float[attributes.length][4];
        boolean[] enabled = new boolean[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i] < 0) continue;
            enabled[i] = GL46C.glGetVertexAttribi(attributes[i], GL46C.GL_VERTEX_ATTRIB_ARRAY_ENABLED) != 0;
            GL46C.glGetVertexAttribfv(attributes[i], GL46C.GL_CURRENT_VERTEX_ATTRIB, previousAttributes[i]);
            GL46C.glDisableVertexAttribArray(attributes[i]);
        }
        MaterialBinder binder = (materialId, alpha) -> {
            if (colorLocation >= 0) GL46C.glUniform4f(colorLocation, red, green, blue, alpha);
            else for (int attribute : attributes) {
                if (attribute >= 0) GL46C.glVertexAttrib4f(attribute, 1.0F, 1.0F, 1.0F, alpha);
            }
        };
        try {
            RenderSystem.depthMask(true);
            draw(subMeshDataBuf, subMeshCount, indexElementSize, indexType, textures, alphaResolver,
                    (materialId, alpha) -> alpha >= .999F, binder);
            RenderSystem.depthMask(false);
            draw(subMeshDataBuf, subMeshCount, indexElementSize, indexType, textures, alphaResolver,
                    (materialId, alpha) -> alpha < .999F, binder);
        } finally {
            RenderSystem.depthMask(depthMask);
            if (colorLocation >= 0) GL46C.glUniform4fv(colorLocation, previousUniform);
            for (int i = 0; i < attributes.length; i++) {
                if (attributes[i] < 0) continue;
                GL46C.glVertexAttrib4fv(attributes[i], previousAttributes[i]);
                if (enabled[i]) GL46C.glEnableVertexAttribArray(attributes[i]);
            }
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

            float materialAlpha = EntityRenderOpacity.clamp(alphaResolver.resolve(materialId, alpha));
            float effectiveAlpha = EntityRenderOpacity.apply(materialAlpha);
            if (!visible || materialAlpha < 0.001f || effectiveAlpha <= 0.0f) {
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
