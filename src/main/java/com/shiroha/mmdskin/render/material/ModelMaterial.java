package com.shiroha.mmdskin.render.material;

import java.util.Locale;

/** MMD 模型材质定义。 */
public class ModelMaterial {
    public static final int MATERIAL_GENERIC = 0;
    public static final int MATERIAL_SKIN = 1;
    public static final int MATERIAL_HAIR = 2;
    public static final int MATERIAL_CLOTH = 3;
    public static final int MATERIAL_EYE = 4;
    public static final int MATERIAL_METAL = 5;
    public static final int MATERIAL_WET = 6;

    private static final String[] FACIAL_TOKENS = {
            "eye", "eyes", "eyeline", "eyelash", "eyelid", "iris", "pupil", "brow", "eyebrow",
            "mouth", "lip", "teeth", "tooth", "tongue", "gum", "lash", "highlight", "eyeshadow",
            "瞳", "目", "眉", "睫", "口", "唇", "牙", "舌", "ハイライト", "まつげ", "くち",
            "くちびる", "アイ", "アイライン", "アイラッシュ"
    };

    public int tex = 0;
    public int toonTex = 0;
    public int sphereTex = 0;
    public boolean hasAlpha = false;
    public String name = "";
    public String texturePath = "";
    public String toonTexturePath = "";
    public String sphereTexturePath = "";
    public boolean ownsTexture = false;
    public float diffuseR = 1.0f;
    public float diffuseG = 1.0f;
    public float diffuseB = 1.0f;
    public float diffuseA = 1.0f;
    public float specularR = 0.0f;
    public float specularG = 0.0f;
    public float specularB = 0.0f;
    public float specularStrength = 0.0f;
    public float ambientR = 0.5f;
    public float ambientG = 0.5f;
    public float ambientB = 0.5f;
    public float edgeR = 0.0f;
    public float edgeG = 0.0f;
    public float edgeB = 0.0f;
    public float edgeA = 1.0f;
    public float edgeScale = 1.0f;
    public boolean edgeEnabled = false;
    public int sphereMode = 0;
    public float studioSpecular = 0.02f;
    public float studioShininess = 36.0f;
    public int studioMaterialType = MATERIAL_GENERIC;
    public float studioRoughness = 0.54f;
    public float studioMetallic = 0.0f;
    public float studioReflectance = 0.55f;
    public float studioSubsurface = 0.0f;
    public float studioAnisotropy = 0.0f;

    private Boolean cachedIsFacialFeature;

    public boolean isFacialFeature() {
        if (cachedIsFacialFeature == null) {
            cachedIsFacialFeature = containsFacialToken(name) || containsFacialToken(texturePath);
        }
        return cachedIsFacialFeature;
    }

    public void apply(PmxMaterialParser.Material material) {
        if (name == null || name.isEmpty()) {
            name = material.name();
        }
        diffuseR = material.diffuseR();
        diffuseG = material.diffuseG();
        diffuseB = material.diffuseB();
        diffuseA = material.diffuseA();
        specularR = material.specularR();
        specularG = material.specularG();
        specularB = material.specularB();
        specularStrength = material.specularStrength();
        ambientR = material.ambientR();
        ambientG = material.ambientG();
        ambientB = material.ambientB();
        edgeR = material.edgeR();
        edgeG = material.edgeG();
        edgeB = material.edgeB();
        edgeA = material.edgeA();
        edgeScale = material.edgeScale();
        edgeEnabled = (material.drawFlags() & 0x10) != 0;
        sphereMode = material.sphereMode();
    }

    public void updateStudioDefaults() {
        updateStudioDefaults("");
    }

    public void updateStudioDefaults(String modelHint) {
        String value = ((name == null ? "" : name) + " "
                + (texturePath == null ? "" : texturePath) + " "
                + (toonTexturePath == null ? "" : toonTexturePath) + " "
                + (modelHint == null ? "" : modelHint)).toLowerCase(Locale.ROOT);
        studioMaterialType = MATERIAL_GENERIC;
        studioRoughness = 0.54f;
        studioMetallic = 0.0f;
        studioReflectance = 0.55f;
        studioSubsurface = 0.0f;
        studioAnisotropy = 0.0f;
        studioSpecular = 0.02f;
        studioShininess = 36.0f;

        if (containsAny(value, "blood", "血")) {
            studioMaterialType = MATERIAL_WET;
            studioRoughness = 0.24f;
            studioReflectance = 0.9f;
            studioSpecular = 0.12f;
            studioShininess = 96.0f;
        } else if (containsAny(value, "hitomi", "sirome", "pupil", "iris", "stareyes", "tear", "瞳")) {
            studioMaterialType = MATERIAL_EYE;
            studioRoughness = 0.16f;
            studioReflectance = 1.15f;
            studioSpecular = 0.12f;
            studioShininess = 92.0f;
        } else if (containsAny(value, "hair", "髪", "发", "hair_toon")) {
            studioMaterialType = MATERIAL_HAIR;
            studioRoughness = 0.34f;
            studioReflectance = 0.82f;
            studioSubsurface = 0.08f;
            studioAnisotropy = 0.75f;
            studioSpecular = 0.07f;
            studioShininess = 64.0f;
        } else if (containsAny(value, "mayuge", "eyeline", "noseline", "eyebrow", "brow", "lash", "眉", "睫")) {
            studioRoughness = 0.72f;
            studioReflectance = 0.16f;
            studioSpecular = 0.01f;
            studioShininess = 24.0f;
        } else if (containsAny(value, "tooth", "teeth", "牙")) {
            studioRoughness = 0.24f;
            studioReflectance = 0.72f;
            studioSpecular = 0.08f;
            studioShininess = 80.0f;
        } else if (containsAny(value, "metal", "blade", "knife", "buckle", "zipper", "steel", "iron", "金属", "鉄", "刀", "刃")) {
            studioMaterialType = MATERIAL_METAL;
            studioRoughness = 0.28f;
            studioMetallic = 0.82f;
            studioReflectance = 0.95f;
            studioSpecular = 0.18f;
            studioShininess = 110.0f;
        } else if (containsAny(value, "cloth_toon", "cloth", "mant", "hood", "服", "布")) {
            studioMaterialType = MATERIAL_CLOTH;
            studioRoughness = 0.8f;
            studioReflectance = 0.25f;
            studioSpecular = 0.015f;
            studioShininess = 28.0f;
        } else if (containsAny(value, "skin_toon", "face", "cf_m_body", "blush", "skin", "肌", "皮肤")) {
            studioMaterialType = MATERIAL_SKIN;
            studioRoughness = 0.58f;
            studioReflectance = 0.55f;
            studioSubsurface = 0.5f;
            studioSpecular = 0.025f;
            studioShininess = 48.0f;
        }
    }

    private static boolean containsFacialToken(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String token : FACIAL_TOKENS) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
