package com.shiroha.mmdskin.render.shader;

import com.shiroha.mmdskin.util.AssetsUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL46C;

import java.nio.FloatBuffer;

/**
 * Toon 着色器抽象基类。
 */
public abstract class ToonShaderBase {
    protected static final Logger logger = LogManager.getLogger();

    protected int mainProgram = 0;
    protected int outlineProgram = 0;
    protected boolean initialized = false;

    protected static final String MAIN_FRAGMENT_SHADER_BODY =
            AssetsUtil.getAssetsAsString("shader/toon_main_body.frag.glsl");

    protected static final String OUTLINE_FRAGMENT_SHADER_BODY =
            AssetsUtil.getAssetsAsString("shader/toon_outline_body.frag.glsl");

    protected int projMatLocation = -1;
    protected int modelViewMatLocation = -1;
    protected int sampler0Location = -1;
    protected int samplerToonLocation = -1;
    protected int samplerSphereLocation = -1;
    protected int hasToonLocation = -1;
    protected int sphereModeLocation = -1;
    protected int lightIntensityLocation = -1;
    protected int toonLevelsLocation = -1;
    protected int rimPowerLocation = -1;
    protected int rimIntensityLocation = -1;
    protected int shadowColorLocation = -1;
    protected int specularPowerLocation = -1;
    protected int specularIntensityLocation = -1;
    protected int lightDirLocation = -1;
    protected int alphaCutoffLocation = -1;
    protected int materialDiffuseLocation = -1;
    protected int materialAmbientLocation = -1;
    protected int materialSpecularLocation = -1;
    protected int materialAlphaLocation = -1;
    protected int textureTintLocation = -1;
    protected int sphereTintLocation = -1;
    protected int toonTintLocation = -1;
    protected int exposureLocation = -1;
    protected int fogStartLocation = -1;
    protected int fogEndLocation = -1;
    protected int fogColorLocation = -1;
    protected int fogShapeLocation = -1;
    protected int renderModeLocation = -1;
    protected int materialTypeLocation = -1;
    protected int materialRoughnessLocation = -1;
    protected int materialMetallicLocation = -1;
    protected int materialReflectanceLocation = -1;
    protected int materialSubsurfaceLocation = -1;
    protected int materialAnisotropyLocation = -1;
    protected int globalRoughnessLocation = -1;
    protected int globalSpecularLocation = -1;
    protected int softShadowLocation = -1;
    protected int mmeRimPowerLocation = -1;
    protected int mmeRimIntensityLocation = -1;
    protected int globalSubsurfaceLocation = -1;
    protected int globalHairAnisotropyLocation = -1;
    protected int fillLightLocation = -1;

    protected int outlineProjMatLocation = -1;
    protected int outlineModelViewMatLocation = -1;
    protected int outlineWidthLocation = -1;
    protected int outlineColorLocation = -1;
    protected int outlineSampler0Location = -1;
    protected int outlineAlphaCutoffLocation = -1;
    protected int outlineAlphaLocation = -1;

    protected int positionLocation = -1;
    protected int normalLocation = -1;
    protected int uv0Location = -1;
    protected int outlinePositionLocation = -1;
    protected int outlineNormalLocation = -1;
    protected int outlineUv0Location = -1;

    protected abstract String getMainVertexShader();

    protected abstract String getOutlineVertexShader();

    protected abstract void onInitialized();

    protected abstract String getShaderName();

    public boolean init() {
        if (initialized) return true;

        try {

            mainProgram = compileProgram(getMainVertexShader(), MAIN_FRAGMENT_SHADER_BODY,
                                        getShaderName() + "主着色器");
            if (mainProgram == 0) return false;

            outlineProgram = compileProgram(getOutlineVertexShader(), OUTLINE_FRAGMENT_SHADER_BODY,
                                            getShaderName() + "描边着色器");
            if (outlineProgram == 0) {
                GL46C.glDeleteProgram(mainProgram);
                mainProgram = 0;
                return false;
            }

            initCommonUniforms();

            initCommonAttributes();

            onInitialized();

            initialized = true;
            return true;

        } catch (Exception e) {
            logger.error("{} 初始化异常", getShaderName(), e);
            return false;
        }
    }

    private void initCommonUniforms() {

        projMatLocation = GL46C.glGetUniformLocation(mainProgram, "ProjMat");
        modelViewMatLocation = GL46C.glGetUniformLocation(mainProgram, "ModelViewMat");
        sampler0Location = GL46C.glGetUniformLocation(mainProgram, "Sampler0");
        samplerToonLocation = GL46C.glGetUniformLocation(mainProgram, "SamplerToon");
        samplerSphereLocation = GL46C.glGetUniformLocation(mainProgram, "SamplerSphere");
        hasToonLocation = GL46C.glGetUniformLocation(mainProgram, "HasToon");
        sphereModeLocation = GL46C.glGetUniformLocation(mainProgram, "SphereMode");
        lightIntensityLocation = GL46C.glGetUniformLocation(mainProgram, "LightIntensity");
        toonLevelsLocation = GL46C.glGetUniformLocation(mainProgram, "ToonLevels");
        rimPowerLocation = GL46C.glGetUniformLocation(mainProgram, "RimPower");
        rimIntensityLocation = GL46C.glGetUniformLocation(mainProgram, "RimIntensity");
        shadowColorLocation = GL46C.glGetUniformLocation(mainProgram, "ShadowColor");
        specularPowerLocation = GL46C.glGetUniformLocation(mainProgram, "SpecularPower");
        specularIntensityLocation = GL46C.glGetUniformLocation(mainProgram, "SpecularIntensity");
        lightDirLocation = GL46C.glGetUniformLocation(mainProgram, "LightDir");
        alphaCutoffLocation = GL46C.glGetUniformLocation(mainProgram, "AlphaCutoff");
        materialDiffuseLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialDiffuse");
        materialAmbientLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialAmbient");
        materialSpecularLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialSpecular");
        materialAlphaLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialAlpha");
        textureTintLocation = GL46C.glGetUniformLocation(mainProgram, "TextureTint");
        sphereTintLocation = GL46C.glGetUniformLocation(mainProgram, "SphereTint");
        toonTintLocation = GL46C.glGetUniformLocation(mainProgram, "ToonTint");
        exposureLocation = GL46C.glGetUniformLocation(mainProgram, "Exposure");
        fogStartLocation = GL46C.glGetUniformLocation(mainProgram, "FogStart");
        fogEndLocation = GL46C.glGetUniformLocation(mainProgram, "FogEnd");
        fogColorLocation = GL46C.glGetUniformLocation(mainProgram, "FogColor");
        fogShapeLocation = GL46C.glGetUniformLocation(mainProgram, "FogShape");
        renderModeLocation = GL46C.glGetUniformLocation(mainProgram, "RenderMode");
        materialTypeLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialType");
        materialRoughnessLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialRoughness");
        materialMetallicLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialMetallic");
        materialReflectanceLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialReflectance");
        materialSubsurfaceLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialSubsurface");
        materialAnisotropyLocation = GL46C.glGetUniformLocation(mainProgram, "MaterialAnisotropy");
        globalRoughnessLocation = GL46C.glGetUniformLocation(mainProgram, "GlobalRoughness");
        globalSpecularLocation = GL46C.glGetUniformLocation(mainProgram, "GlobalSpecular");
        softShadowLocation = GL46C.glGetUniformLocation(mainProgram, "SoftShadow");
        mmeRimPowerLocation = GL46C.glGetUniformLocation(mainProgram, "MmeRimPower");
        mmeRimIntensityLocation = GL46C.glGetUniformLocation(mainProgram, "MmeRimIntensity");
        globalSubsurfaceLocation = GL46C.glGetUniformLocation(mainProgram, "GlobalSubsurface");
        globalHairAnisotropyLocation = GL46C.glGetUniformLocation(mainProgram, "GlobalHairAnisotropy");
        fillLightLocation = GL46C.glGetUniformLocation(mainProgram, "FillLight");

        outlineProjMatLocation = GL46C.glGetUniformLocation(outlineProgram, "ProjMat");
        outlineModelViewMatLocation = GL46C.glGetUniformLocation(outlineProgram, "ModelViewMat");
        outlineWidthLocation = GL46C.glGetUniformLocation(outlineProgram, "OutlineWidth");
        outlineColorLocation = GL46C.glGetUniformLocation(outlineProgram, "OutlineColor");
        outlineSampler0Location = GL46C.glGetUniformLocation(outlineProgram, "Sampler0");
        outlineAlphaCutoffLocation = GL46C.glGetUniformLocation(outlineProgram, "AlphaCutoff");
        outlineAlphaLocation = GL46C.glGetUniformLocation(outlineProgram, "OutlineAlpha");
    }

    private void initCommonAttributes() {

        positionLocation = GL46C.glGetAttribLocation(mainProgram, "Position");
        normalLocation = GL46C.glGetAttribLocation(mainProgram, "Normal");
        uv0Location = GL46C.glGetAttribLocation(mainProgram, "UV0");

        outlinePositionLocation = GL46C.glGetAttribLocation(outlineProgram, "Position");
        outlineNormalLocation = GL46C.glGetAttribLocation(outlineProgram, "Normal");
        outlineUv0Location = GL46C.glGetAttribLocation(outlineProgram, "UV0");
    }

    protected int compileProgram(String vertexSource, String fragmentSource, String name) {
        return ShaderCompiler.compileRenderProgram(vertexSource, fragmentSource, name);
    }

    public void useMain() {
        if (mainProgram > 0) {
            GL46C.glUseProgram(mainProgram);
        }
    }

    public void useOutline() {
        if (outlineProgram > 0) {
            GL46C.glUseProgram(outlineProgram);
        }
    }

    public void setProjectionMatrix(FloatBuffer matrix) {
        if (projMatLocation >= 0) {
            matrix.position(0);
            GL46C.glUniformMatrix4fv(projMatLocation, false, matrix);
        }
    }

    public void setModelViewMatrix(FloatBuffer matrix) {
        if (modelViewMatLocation >= 0) {
            matrix.position(0);
            GL46C.glUniformMatrix4fv(modelViewMatLocation, false, matrix);
        }
    }

    public void setSampler0(int textureUnit) {
        if (sampler0Location >= 0) {
            GL46C.glUniform1i(sampler0Location, textureUnit);
        }
    }

    public void setMaterialTextures(int toonTextureUnit, int sphereTextureUnit, boolean hasToon, int sphereMode) {
        if (samplerToonLocation >= 0) {
            GL46C.glUniform1i(samplerToonLocation, toonTextureUnit);
        }
        if (samplerSphereLocation >= 0) {
            GL46C.glUniform1i(samplerSphereLocation, sphereTextureUnit);
        }
        if (hasToonLocation >= 0) {
            GL46C.glUniform1i(hasToonLocation, hasToon ? 1 : 0);
        }
        if (sphereModeLocation >= 0) {
            GL46C.glUniform1i(sphereModeLocation, sphereMode);
        }
    }

    public void setMaterialDiffuse(float r, float g, float b) {
        if (materialDiffuseLocation >= 0) {
            GL46C.glUniform3f(materialDiffuseLocation, r, g, b);
        }
    }

    public void setMaterialAmbient(float r, float g, float b) {
        if (materialAmbientLocation >= 0) {
            GL46C.glUniform3f(materialAmbientLocation, r, g, b);
        }
    }

    public void setMaterialSpecular(float r, float g, float b, float power, float intensity) {
        if (materialSpecularLocation >= 0) {
            GL46C.glUniform3f(materialSpecularLocation, r, g, b);
        }
        setSpecular(power, intensity);
    }

    public void setMaterialAlpha(float alpha) {
        if (materialAlphaLocation >= 0) {
            GL46C.glUniform1f(materialAlphaLocation, alpha);
        }
    }

    public void setMaterialTints(float textureR, float textureG, float textureB, float textureA,
                                 float sphereR, float sphereG, float sphereB, float sphereA,
                                 float toonR, float toonG, float toonB, float toonA) {
        if (textureTintLocation >= 0) {
            GL46C.glUniform4f(textureTintLocation, textureR, textureG, textureB, textureA);
        }
        if (sphereTintLocation >= 0) {
            GL46C.glUniform4f(sphereTintLocation, sphereR, sphereG, sphereB, sphereA);
        }
        if (toonTintLocation >= 0) {
            GL46C.glUniform4f(toonTintLocation, toonR, toonG, toonB, toonA);
        }
    }

    public void setExposure(float exposure) {
        if (exposureLocation >= 0) {
            GL46C.glUniform1f(exposureLocation, exposure);
        }
    }

    public void setRenderMode(int mode) {
        if (renderModeLocation >= 0) {
            GL46C.glUniform1i(renderModeLocation, mode);
        }
    }

    public void setMmeParameters(float roughness, float specular, float softShadow,
                                 float rimPower, float rimIntensity, float subsurface,
                                 float hairAnisotropy, float fillLight) {
        if (globalRoughnessLocation >= 0) GL46C.glUniform1f(globalRoughnessLocation, roughness);
        if (globalSpecularLocation >= 0) GL46C.glUniform1f(globalSpecularLocation, specular);
        if (softShadowLocation >= 0) GL46C.glUniform1f(softShadowLocation, softShadow);
        if (mmeRimPowerLocation >= 0) GL46C.glUniform1f(mmeRimPowerLocation, rimPower);
        if (mmeRimIntensityLocation >= 0) GL46C.glUniform1f(mmeRimIntensityLocation, rimIntensity);
        if (globalSubsurfaceLocation >= 0) GL46C.glUniform1f(globalSubsurfaceLocation, subsurface);
        if (globalHairAnisotropyLocation >= 0) GL46C.glUniform1f(globalHairAnisotropyLocation, hairAnisotropy);
        if (fillLightLocation >= 0) GL46C.glUniform1f(fillLightLocation, fillLight);
    }

    public void setStudioMaterial(int type, float roughness, float metallic,
                                  float reflectance, float subsurface, float anisotropy) {
        if (materialTypeLocation >= 0) GL46C.glUniform1i(materialTypeLocation, type);
        if (materialRoughnessLocation >= 0) GL46C.glUniform1f(materialRoughnessLocation, roughness);
        if (materialMetallicLocation >= 0) GL46C.glUniform1f(materialMetallicLocation, metallic);
        if (materialReflectanceLocation >= 0) GL46C.glUniform1f(materialReflectanceLocation, reflectance);
        if (materialSubsurfaceLocation >= 0) GL46C.glUniform1f(materialSubsurfaceLocation, subsurface);
        if (materialAnisotropyLocation >= 0) GL46C.glUniform1f(materialAnisotropyLocation, anisotropy);
    }

    public void setFog(float start, float end, float r, float g, float b, float a, int shape) {
        if (fogStartLocation >= 0) {
            GL46C.glUniform1f(fogStartLocation, start);
        }
        if (fogEndLocation >= 0) {
            GL46C.glUniform1f(fogEndLocation, end);
        }
        if (fogColorLocation >= 0) {
            GL46C.glUniform4f(fogColorLocation, r, g, b, a);
        }
        if (fogShapeLocation >= 0) {
            GL46C.glUniform1i(fogShapeLocation, shape);
        }
    }

    public void setLightIntensity(float intensity) {
        if (lightIntensityLocation >= 0) {
            GL46C.glUniform1f(lightIntensityLocation, intensity);
        }
    }

    public void setToonLevels(int levels) {
        if (toonLevelsLocation >= 0) {
            GL46C.glUniform1i(toonLevelsLocation, Math.max(2, Math.min(5, levels)));
        }
    }

    public void setRimLight(float power, float intensity) {
        if (rimPowerLocation >= 0) {
            GL46C.glUniform1f(rimPowerLocation, power);
        }
        if (rimIntensityLocation >= 0) {
            GL46C.glUniform1f(rimIntensityLocation, intensity);
        }
    }

    public void setShadowColor(float r, float g, float b) {
        if (shadowColorLocation >= 0) {
            GL46C.glUniform3f(shadowColorLocation, r, g, b);
        }
    }

    public void setSpecular(float power, float intensity) {
        if (specularPowerLocation >= 0) {
            GL46C.glUniform1f(specularPowerLocation, power);
        }
        if (specularIntensityLocation >= 0) {
            GL46C.glUniform1f(specularIntensityLocation, intensity);
        }
    }

    public void setLightDirection(float x, float y, float z) {
        if (lightDirLocation >= 0) {
            GL46C.glUniform3f(lightDirLocation, x, y, z);
        }
    }

    public void setAlphaCutoff(float cutoff) {
        if (alphaCutoffLocation >= 0) {
            GL46C.glUniform1f(alphaCutoffLocation, cutoff);
        }
    }

    public void setOutlineProjectionMatrix(FloatBuffer matrix) {
        if (outlineProjMatLocation >= 0) {
            matrix.position(0);
            GL46C.glUniformMatrix4fv(outlineProjMatLocation, false, matrix);
        }
    }

    public void setOutlineModelViewMatrix(FloatBuffer matrix) {
        if (outlineModelViewMatLocation >= 0) {
            matrix.position(0);
            GL46C.glUniformMatrix4fv(outlineModelViewMatLocation, false, matrix);
        }
    }

    public void setOutlineWidth(float width) {
        if (outlineWidthLocation >= 0) {
            GL46C.glUniform1f(outlineWidthLocation, width);
        }
    }

    public void setOutlineColor(float r, float g, float b) {
        if (outlineColorLocation >= 0) {
            GL46C.glUniform3f(outlineColorLocation, r, g, b);
        }
    }

    public void setOutlineSampler0(int textureUnit) {
        if (outlineSampler0Location >= 0) {
            GL46C.glUniform1i(outlineSampler0Location, textureUnit);
        }
    }

    public void setOutlineAlphaCutoff(float cutoff) {
        if (outlineAlphaCutoffLocation >= 0) {
            GL46C.glUniform1f(outlineAlphaCutoffLocation, cutoff);
        }
    }

    public void setOutlineAlpha(float alpha) {
        if (outlineAlphaLocation >= 0) {
            GL46C.glUniform1f(outlineAlphaLocation, alpha);
        }
    }

    public int getMainProgram() { return mainProgram; }
    public int getOutlineProgram() { return outlineProgram; }

    public int getPositionLocation() { return positionLocation; }
    public int getNormalLocation() { return normalLocation; }
    public int getUv0Location() { return uv0Location; }

    public int getOutlinePositionLocation() { return outlinePositionLocation; }
    public int getOutlineNormalLocation() { return outlineNormalLocation; }
    public int getOutlineUv0Location() { return outlineUv0Location; }

    public boolean isInitialized() { return initialized; }

    public void cleanup() {
        if (mainProgram > 0) {
            GL46C.glDeleteProgram(mainProgram);
            mainProgram = 0;
        }
        if (outlineProgram > 0) {
            GL46C.glDeleteProgram(outlineProgram);
            outlineProgram = 0;
        }
        initialized = false;
    }
}
