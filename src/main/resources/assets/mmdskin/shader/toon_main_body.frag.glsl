#version 330 core

in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewPos;
in vec3 viewLightDir;
in vec3 viewUpDir;

uniform sampler2D Sampler0;
uniform sampler2D SamplerToon;
uniform sampler2D SamplerSphere;
uniform int HasToon;
uniform int SphereMode;
uniform float LightIntensity;
uniform int ToonLevels;
uniform float RimPower;
uniform float RimIntensity;
uniform vec3 ShadowColor;
uniform float SpecularPower;
uniform float SpecularIntensity;
uniform float AlphaCutoff;
uniform vec3 MaterialDiffuse;
uniform vec3 MaterialAmbient;
uniform vec3 MaterialSpecular;
uniform float MaterialAlpha;
uniform vec4 TextureTint;
uniform vec4 SphereTint;
uniform vec4 ToonTint;
uniform float Exposure;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform int FogShape;
uniform int RenderMode;
uniform int MaterialType;
uniform float MaterialRoughness;
uniform float MaterialMetallic;
uniform float MaterialReflectance;
uniform float MaterialSubsurface;
uniform float MaterialAnisotropy;
uniform float GlobalRoughness;
uniform float GlobalSpecular;
uniform float SoftShadow;
uniform float MmeRimPower;
uniform float MmeRimIntensity;
uniform float GlobalSubsurface;
uniform float GlobalHairAnisotropy;
uniform float FillLight;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 fragData1;
layout(location = 2) out vec4 fragData2;
layout(location = 3) out vec4 fragData3;

const float PI = 3.14159265359;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

vec3 srgbToLinear(vec3 value) {
    return pow(max(value, vec3(0.0)), vec3(2.2));
}

vec3 linearToSrgb(vec3 value) {
    return pow(max(value, vec3(0.0)), vec3(1.0 / 2.2));
}

vec3 acesToneMap(vec3 value) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((value * (a * value + b)) / (value * (c * value + d) + e), 0.0, 1.0);
}

float analyticToon(float halfLambert) {
    float bands = float(max(ToonLevels, 2));
    float edgeSoftness = mix(0.09, 0.045, clamp((bands - 2.0) / 3.0, 0.0, 1.0));
    if (bands <= 2.0) {
        return smoothstep(0.5 - edgeSoftness, 0.5 + edgeSoftness, halfLambert);
    }
    float scaled = halfLambert * (bands - 1.0);
    float stepIndex = floor(scaled);
    float blend = smoothstep(0.5 - edgeSoftness, 0.5 + edgeSoftness, fract(scaled));
    return saturate((stepIndex + blend) / (bands - 1.0));
}

float distributionGgx(vec3 normal, vec3 halfDir, float roughness) {
    float alpha = roughness * roughness;
    float alphaSquared = alpha * alpha;
    float ndoth = saturate(dot(normal, halfDir));
    float denominator = ndoth * ndoth * (alphaSquared - 1.0) + 1.0;
    return alphaSquared / max(PI * denominator * denominator, 0.00001);
}

float geometrySchlickGgx(float ndot, float roughness) {
    float value = roughness + 1.0;
    float k = value * value * 0.125;
    return ndot / max(ndot * (1.0 - k) + k, 0.00001);
}

float geometrySmith(vec3 normal, vec3 viewDir, vec3 lightDir, float roughness) {
    return geometrySchlickGgx(saturate(dot(normal, viewDir)), roughness)
        * geometrySchlickGgx(saturate(dot(normal, lightDir)), roughness);
}

vec3 fresnelSchlick(float cosTheta, vec3 f0) {
    float factor = pow(1.0 - saturate(cosTheta), 5.0);
    return f0 + (1.0 - f0) * factor;
}

vec3 fresnelSchlickRoughness(float cosTheta, vec3 f0, float roughness) {
    float factor = pow(1.0 - saturate(cosTheta), 5.0);
    return f0 + (max(vec3(1.0 - roughness), f0) - f0) * factor;
}

vec3 applySphereMap(vec3 color, vec3 normal) {
    if (SphereMode == 0) {
        return color;
    }
    vec2 sphereUv = vec2(normal.x * 0.5 + 0.5, -normal.y * 0.5 + 0.5);
    if (SphereMode == 3) {
        sphereUv = texCoord0;
    }
    vec3 sphereColor = srgbToLinear(texture(SamplerSphere, sphereUv).rgb * SphereTint.rgb);
    if (SphereMode == 2) {
        return color + sphereColor * SphereTint.a;
    }
    return color * mix(vec3(1.0), sphereColor, SphereTint.a);
}

vec3 renderToon(vec3 albedo, vec3 normal, vec3 lightDir, vec3 viewDir) {
    float ndotl = dot(normal, lightDir);
    float halfLambert = saturate(ndotl * 0.5 + 0.5);
    vec3 toonColor;
    if (HasToon != 0) {
        vec2 toonUv = vec2(0.5, clamp(1.0 - halfLambert, 0.002, 0.998));
        toonColor = srgbToLinear(texture(SamplerToon, toonUv).rgb * ToonTint.rgb);
    } else {
        float ramp = analyticToon(halfLambert);
        toonColor = mix(srgbToLinear(ShadowColor), vec3(1.0), ramp);
    }

    float upward = saturate(dot(normal, normalize(viewUpDir)) * 0.5 + 0.5);
    vec3 groundLight = srgbToLinear(vec3(0.20, 0.18, 0.20));
    vec3 skyLight = srgbToLinear(vec3(0.52, 0.61, 0.72));
    vec3 hemisphere = mix(groundLight, skyLight, upward);
    float sceneLight = mix(0.52, 1.0, saturate(LightIntensity));
    vec3 ambientWeight = mix(vec3(0.35), clamp(MaterialAmbient, 0.0, 2.0), 0.32);
    vec3 result = albedo * toonColor * sceneLight;
    result += albedo * hemisphere * ambientWeight * mix(0.12, 0.24, saturate(LightIntensity));

    vec3 halfDir = normalize(lightDir + viewDir);
    float specAngle = saturate(dot(normal, halfDir));
    float specular = pow(specAngle, max(SpecularPower, 1.0));
    float specularGate = smoothstep(0.32, 0.58, halfLambert);
    result += srgbToLinear(clamp(MaterialSpecular, 0.0, 1.0)) * specular * SpecularIntensity * specularGate;

    float fresnel = 1.0 - saturate(dot(viewDir, normal));
    float rim = pow(fresnel, max(RimPower, 1.0));
    float rimMask = smoothstep(0.45, 0.9, rim) * (1.0 - halfLambert * 0.55);
    result += albedo * rimMask * RimIntensity * 0.6;
    return result;
}

vec3 renderMme(vec3 albedo, vec3 normal, vec3 lightDir, vec3 viewDir) {
    vec3 halfDir = normalize(lightDir + viewDir);
    vec3 upDir = normalize(viewUpDir);
    float ndotv = max(saturate(dot(normal, viewDir)), 0.001);
    float rawNdotl = dot(normal, lightDir);
    float softNdotl = smoothstep(-0.22, 0.42, rawNdotl);
    float diffuseNdotl = mix(saturate(rawNdotl), softNdotl, saturate(SoftShadow));

    float roughnessScale = mix(0.72, 1.28, saturate(GlobalRoughness));
    float roughness = clamp(MaterialRoughness * roughnessScale, 0.055, 0.98);
    float metallic = saturate(MaterialMetallic);
    float reflectance = clamp(MaterialReflectance, 0.0, 1.5);
    vec3 specularTint = clamp(MaterialSpecular, 0.0, 1.0);
    if (dot(specularTint, specularTint) < 0.0001) {
        specularTint = vec3(1.0);
    }
    vec3 dielectric = vec3(0.04 * mix(0.72, 1.35, saturate(reflectance)));
    dielectric *= mix(vec3(1.0), specularTint, 0.3);
    vec3 f0 = mix(dielectric, albedo, metallic);

    float distribution = distributionGgx(normal, halfDir, roughness);
    float geometry = geometrySmith(normal, viewDir, lightDir, roughness);
    vec3 fresnel = fresnelSchlick(saturate(dot(halfDir, viewDir)), f0);
    vec3 specular = distribution * geometry * fresnel
        / max(4.0 * ndotv * max(saturate(rawNdotl), 0.04), 0.001);
    specular *= GlobalSpecular * reflectance;

    float subsurface = saturate(MaterialSubsurface * GlobalSubsurface);
    float wrappedNdotl = saturate((rawNdotl + 0.28) / 1.28);
    diffuseNdotl = mix(diffuseNdotl, wrappedNdotl, subsurface * 0.55);
    vec3 diffuseWeight = (vec3(1.0) - fresnel) * (1.0 - metallic);
    vec3 diffuse = diffuseWeight * albedo / PI;

    float sceneLight = saturate(LightIntensity);
    float keyEnergy = mix(0.72, 2.35, sceneLight);
    vec3 keyColor = srgbToLinear(vec3(1.0, 0.94, 0.90));
    vec3 result = (diffuse + specular) * keyColor * keyEnergy * diffuseNdotl;

    float upward = saturate(dot(normal, upDir) * 0.5 + 0.5);
    vec3 groundLight = srgbToLinear(vec3(0.17, 0.14, 0.16));
    vec3 skyLight = srgbToLinear(vec3(0.54, 0.64, 0.78));
    vec3 hemisphere = mix(groundLight, skyLight, upward);
    vec3 ambientWeight = mix(vec3(0.52), clamp(MaterialAmbient, 0.0, 2.0), 0.22);
    float ambientEnergy = mix(0.52, 0.78, sceneLight);
    result += albedo * hemisphere * ambientWeight * ambientEnergy;

    vec3 fillDir = normalize(vec3(-lightDir.x, max(lightDir.y, 0.2), -lightDir.z));
    float fillNdotl = saturate(dot(normal, fillDir) * 0.5 + 0.5);
    vec3 fillColor = srgbToLinear(vec3(0.50, 0.62, 0.82));
    result += albedo * fillColor * fillNdotl * FillLight * 0.28;

    vec3 environmentFresnel = fresnelSchlickRoughness(ndotv, f0, roughness);
    float environmentStrength = mix(0.08, 0.28, 1.0 - roughness) * GlobalSpecular;
    environmentStrength *= mix(1.0, 1.8, metallic);
    result += hemisphere * environmentFresnel * environmentStrength;

    float backLight = pow(saturate(dot(-normal, lightDir)), 1.7);
    vec3 scatterColor = albedo * vec3(1.24, 0.72, 0.62);
    result += scatterColor * backLight * subsurface * keyEnergy * 0.2;

    float anisotropy = saturate(MaterialAnisotropy * GlobalHairAnisotropy);
    vec3 tangent = upDir - normal * dot(upDir, normal);
    float tangentLength = length(tangent);
    if (tangentLength < 0.001) {
        tangent = normalize(cross(normal, vec3(1.0, 0.0, 0.0)));
    } else {
        tangent /= tangentLength;
    }
    vec3 shiftedTangent = normalize(tangent + normal * 0.16);
    float tangentDotHalf = clamp(dot(shiftedTangent, halfDir), -1.0, 1.0);
    float strandHighlight = pow(sqrt(max(1.0 - tangentDotHalf * tangentDotHalf, 0.0)), mix(18.0, 72.0, 1.0 - roughness));
    vec3 hairColor = mix(albedo, srgbToLinear(vec3(1.0, 0.78, 0.70)), 0.42);
    result += hairColor * strandHighlight * anisotropy * GlobalSpecular * diffuseNdotl * 0.34;

    if (MaterialType == 4 || MaterialType == 6) {
        float clearCoat = pow(saturate(dot(normal, halfDir)), mix(70.0, 220.0, 1.0 - roughness));
        result += vec3(clearCoat * GlobalSpecular * diffuseNdotl * 0.22);
    }

    float rim = pow(1.0 - ndotv, max(MmeRimPower, 0.5));
    float rimMask = smoothstep(0.06, 0.82, rim) * mix(1.0, 0.58, saturate(rawNdotl));
    vec3 rimColor = mix(skyLight, albedo, 0.28);
    result += rimColor * rimMask * MmeRimIntensity;
    return result;
}

float fogDistance(vec3 position) {
    if (FogShape == 0) {
        return length(position);
    }
    return max(length(position.xz), abs(position.y));
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord0) * TextureTint;
    float alpha = texColor.a * MaterialAlpha;
    if (alpha < AlphaCutoff) {
        discard;
    }

    vec3 viewDir = normalize(-viewPos);
    vec3 normal = faceforward(normalize(viewNormal), -viewDir, normalize(viewNormal));
    vec3 lightDir = normalize(viewLightDir);
    vec3 albedo = srgbToLinear(texColor.rgb) * max(MaterialDiffuse, vec3(0.0));
    vec3 finalColor = RenderMode == 1
        ? renderMme(albedo, normal, lightDir, viewDir)
        : renderToon(albedo, normal, lightDir, viewDir);
    finalColor = applySphereMap(finalColor, normal);

    vec3 displayColor = linearToSrgb(acesToneMap(finalColor * Exposure));
    float fogRange = max(FogEnd - FogStart, 0.001);
    float fogAmount = saturate((fogDistance(viewPos) - FogStart) / fogRange);
    displayColor = mix(displayColor, FogColor.rgb, fogAmount);

    fragColor = vec4(displayColor, alpha);
    fragData1 = vec4(normal * 0.5 + 0.5, 1.0);
    fragData2 = vec4(0.0, 0.0, 0.0, 1.0);
    fragData3 = vec4(0.0, 0.0, 0.0, 1.0);
}
