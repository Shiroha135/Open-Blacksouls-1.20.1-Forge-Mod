package com.shiroha.mmdskin.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.shiroha.mmdskin.NativeFunc;
import com.shiroha.mmdskin.config.ConfigManager;
import com.shiroha.mmdskin.render.material.ModelMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46C;

/**
 * Toon 渲染辅助类。
 */
public class ToonRenderHelper {

    private static final ToonConfig toonConfig = ToonConfig.getInstance();
    private static final float ALPHA_CUTOFF = 0.1f;

    public static void setupToonUniforms(ToonShaderBase shader, float lightIntensity, Vector3f lightDirection) {
        boolean mmeEnabled = ConfigManager.isMmeRenderingEnabled();
        float lightX = 0.35f;
        float lightY = 0.85f;
        float lightZ = -0.4f;
        if (lightDirection != null) {
            float lenSq = lightDirection.x * lightDirection.x
                    + lightDirection.y * lightDirection.y
                    + lightDirection.z * lightDirection.z;
            if (lenSq > 1.0E-6f) {
                float invLen = (float) (1.0 / Math.sqrt(lenSq));
                lightX = lightDirection.x * invLen;
                lightY = lightDirection.y * invLen;
                lightZ = lightDirection.z * invLen;
            }
        }

        shader.setSampler0(0);
        shader.setRenderMode(mmeEnabled ? 1 : 0);
        shader.setMaterialTextures(1, 2, false, 0);
        shader.setLightIntensity(lightIntensity);
        shader.setToonLevels(toonConfig.getToonLevels());
        shader.setRimLight(toonConfig.getRimPower(), toonConfig.getRimIntensity());
        shader.setShadowColor(
            toonConfig.getShadowColorR(),
            toonConfig.getShadowColorG(),
            toonConfig.getShadowColorB()
        );
        shader.setSpecular(toonConfig.getSpecularPower(), toonConfig.getSpecularIntensity());
        shader.setLightDirection(lightX, lightY, lightZ);
        shader.setAlphaCutoff(ALPHA_CUTOFF);
        shader.setExposure(mmeEnabled ? ConfigManager.getMmeExposure() : 1.08f);
        shader.setMmeParameters(
                ConfigManager.getMmeRoughness(),
                ConfigManager.getMmeSpecularIntensity(),
                ConfigManager.getMmeSoftShadow(),
                ConfigManager.getMmeRimPower(),
                ConfigManager.getMmeRimIntensity(),
                ConfigManager.getMmeSubsurface(),
                ConfigManager.getMmeHairAnisotropy(),
                ConfigManager.getMmeFillLight()
        );
        float[] fogColor = RenderSystem.getShaderFogColor();
        shader.setFog(
                RenderSystem.getShaderFogStart(),
                RenderSystem.getShaderFogEnd(),
                fogColor[0],
                fogColor[1],
                fogColor[2],
                fogColor[3],
                RenderSystem.getShaderFogShape().getIndex()
        );
    }

    public static void setupOutlineUniforms(ToonShaderBase shader) {
        shader.setOutlineSampler0(0);
        shader.setOutlineAlphaCutoff(ALPHA_CUTOFF);
        shader.setOutlineWidth(toonConfig.getOutlineWidth());
        shader.setOutlineColor(
            toonConfig.getOutlineColorR(),
            toonConfig.getOutlineColorG(),
            toonConfig.getOutlineColorB()
        );
        shader.setOutlineAlpha(1.0f);
    }

    public static void bindMaterial(ToonShaderBase shader,
                                    ModelMaterial material,
                                    int materialId,
                                    float alpha,
                                    MaterialValueResolver resolver) {
        float diffuseR = clamp(resolver.resolve(materialId, 0, material.diffuseR), 0.0f, 4.0f);
        float diffuseG = clamp(resolver.resolve(materialId, 1, material.diffuseG), 0.0f, 4.0f);
        float diffuseB = clamp(resolver.resolve(materialId, 2, material.diffuseB), 0.0f, 4.0f);
        float specularR = clamp(resolver.resolve(materialId, 4, material.specularR), 0.0f, 4.0f);
        float specularG = clamp(resolver.resolve(materialId, 5, material.specularG), 0.0f, 4.0f);
        float specularB = clamp(resolver.resolve(materialId, 6, material.specularB), 0.0f, 4.0f);
        float ambientR = clamp(resolver.resolve(materialId, 8, material.ambientR), 0.0f, 4.0f);
        float ambientG = clamp(resolver.resolve(materialId, 9, material.ambientG), 0.0f, 4.0f);
        float ambientB = clamp(resolver.resolve(materialId, 10, material.ambientB), 0.0f, 4.0f);
        float pmxPower = resolver.resolve(materialId, 7, material.specularStrength);
        float powerScale = clamp(toonConfig.getSpecularPower() / 96.0f, 0.2f, 2.0f);
        float specularLength = (float) Math.sqrt(specularR * specularR + specularG * specularG + specularB * specularB);
        float specularScale = clamp(toonConfig.getSpecularIntensity() / 0.015f, 0.0f, 4.0f);
        float specularIntensity = (specularLength > 0.001f
                ? Math.max(0.035f, Math.min(0.28f, specularLength / 3.0f))
                : material.studioSpecular) * specularScale;
        if (specularLength <= 0.001f) {
            specularR = 1.0f;
            specularG = 1.0f;
            specularB = 1.0f;
        }

        shader.setMaterialDiffuse(diffuseR, diffuseG, diffuseB);
        shader.setMaterialAmbient(ambientR, ambientG, ambientB);
        shader.setMaterialSpecular(
                specularR,
                specularG,
                specularB,
                Math.max(pmxPower, material.studioShininess) * powerScale,
                specularIntensity
        );
        shader.setMaterialAlpha(clamp(alpha, 0.0f, 1.0f));
        shader.setStudioMaterial(
                material.studioMaterialType,
                material.studioRoughness,
                material.studioMetallic,
                material.studioReflectance,
                material.studioSubsurface,
                material.studioAnisotropy
        );
        shader.setMaterialTints(
                resolver.resolve(materialId, 16, 1.0f),
                resolver.resolve(materialId, 17, 1.0f),
                resolver.resolve(materialId, 18, 1.0f),
                resolver.resolve(materialId, 19, 1.0f),
                resolver.resolve(materialId, 20, 1.0f),
                resolver.resolve(materialId, 21, 1.0f),
                resolver.resolve(materialId, 22, 1.0f),
                resolver.resolve(materialId, 23, 1.0f),
                resolver.resolve(materialId, 24, 1.0f),
                resolver.resolve(materialId, 25, 1.0f),
                resolver.resolve(materialId, 26, 1.0f),
                resolver.resolve(materialId, 27, 1.0f)
        );
        shader.setMaterialTextures(1, 2, material.toonTex > 0, material.sphereTex > 0 ? material.sphereMode : 0);

        RenderSystem.activeTexture(GL46C.GL_TEXTURE1);
        RenderSystem.bindTexture(material.toonTex);
        RenderSystem.activeTexture(GL46C.GL_TEXTURE2);
        RenderSystem.bindTexture(material.sphereTex);
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
    }

    public static void bindOutlineMaterial(ToonShaderBase shader,
                                           ModelMaterial material,
                                           int materialId,
                                           float alpha,
                                           MaterialValueResolver resolver) {
        float edgeR = clamp(resolver.resolve(materialId, 11, material.edgeR), 0.0f, 1.0f);
        float edgeG = clamp(resolver.resolve(materialId, 12, material.edgeG), 0.0f, 1.0f);
        float edgeB = clamp(resolver.resolve(materialId, 13, material.edgeB), 0.0f, 1.0f);
        float edgeA = clamp(resolver.resolve(materialId, 14, material.edgeA), 0.0f, 1.0f);
        float edgeScale = Math.max(0.0f, resolver.resolve(materialId, 15, material.edgeScale));
        shader.setOutlineWidth(toonConfig.getOutlineWidth() * edgeScale);
        shader.setOutlineColor(
                edgeR * 0.7f + toonConfig.getOutlineColorR() * 0.3f,
                edgeG * 0.7f + toonConfig.getOutlineColorG() * 0.3f,
                edgeB * 0.7f + toonConfig.getOutlineColorB() * 0.3f
        );
        shader.setOutlineAlpha(clamp(alpha * edgeA, 0.0f, 1.0f));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @FunctionalInterface
    public interface MaterialValueResolver {
        float resolve(int materialId, int component, float baseValue);
    }

    public static void prepareRenderState(int vao) {
        BufferUploader.reset();
        GL46C.glBindVertexArray(vao);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendEquation(GL46C.GL_FUNC_ADD);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    public static void restoreRenderState() {
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, 0);
        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL46C.glBindVertexArray(0);
        GL46C.glUseProgram(0);
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
        BufferUploader.reset();
    }

    public static boolean isOutlineEnabled() {
        return !ConfigManager.isMmeRenderingEnabled() && toonConfig.isOutlineEnabled();
    }

    public static void setupOutlineCulling() {
        GL46C.glCullFace(GL46C.GL_FRONT);
        RenderSystem.enableCull();
    }

    public static void restoreNormalCulling() {
        GL46C.glCullFace(GL46C.GL_BACK);
    }

    public static void drawSubMeshesOutline(NativeFunc nf, long model, int indexElementSize, int indexType) {
        long subMeshCount = nf.GetSubMeshCount(model);
        for (long i = 0; i < subMeshCount; ++i) {
            int materialID = nf.GetSubMeshMaterialID(model, i);
            if (!nf.IsMaterialVisible(model, materialID)) continue;
            if (nf.GetMaterialAlpha(model, materialID) == 0.0f) continue;

            long startPos = (long) nf.GetSubMeshBeginIndex(model, i) * indexElementSize;
            int count = nf.GetSubMeshVertexCount(model, i);
            GL46C.glDrawElements(GL46C.GL_TRIANGLES, count, indexType, startPos);
        }
    }

    public interface MaterialProvider {
        int getTextureId(int materialID);
    }

    public static void drawSubMeshesMain(Minecraft mc, NativeFunc nf, long model,
                                         int indexElementSize, int indexType,
                                         MaterialProvider materialProvider) {
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
        long subMeshCount = nf.GetSubMeshCount(model);

        for (long i = 0; i < subMeshCount; ++i) {
            int materialID = nf.GetSubMeshMaterialID(model, i);
            if (!nf.IsMaterialVisible(model, materialID)) continue;

            float alpha = nf.GetMaterialAlpha(model, materialID);
            if (alpha == 0.0f) continue;

            if (nf.GetMaterialBothFace(model, materialID)) {
                RenderSystem.disableCull();
            } else {
                RenderSystem.enableCull();
            }

            int texId = materialProvider.getTextureId(materialID);
            if (texId == 0) {
                texId = mc.getTextureManager().getTexture(TextureManager.INTENTIONAL_MISSING_TEXTURE).getId();
            }
            RenderSystem.setShaderTexture(0, texId);
            GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, texId);

            long startPos = (long) nf.GetSubMeshBeginIndex(model, i) * indexElementSize;
            int count = nf.GetSubMeshVertexCount(model, i);

            GL46C.glDrawElements(GL46C.GL_TRIANGLES, count, indexType, startPos);
        }
    }

    public static void disableVertexAttribArray(int... locations) {
        for (int loc : locations) {
            if (loc != -1) {
                GL46C.glDisableVertexAttribArray(loc);
            }
        }
    }

    public static void setupFloatVertexAttrib(int location, int vbo, int size, java.nio.ByteBuffer data) {
        if (location != -1) {
            GL46C.glEnableVertexAttribArray(location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, data, GL46C.GL_DYNAMIC_DRAW);
            GL46C.glVertexAttribPointer(location, size, GL46C.GL_FLOAT, false, 0, 0);
        }
    }

    public static void setupIntVertexAttrib(int location, int vbo, int size, java.nio.ByteBuffer data) {
        if (location != -1) {
            GL46C.glEnableVertexAttribArray(location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, data, GL46C.GL_DYNAMIC_DRAW);
            GL46C.glVertexAttribIPointer(location, size, GL46C.GL_INT, 0, 0);
        }
    }
}
