package com.shiroha.mmdskin.config;

public interface IMmeConfig {
    default boolean isMmeRenderingEnabled() { return true; }
    default float getMmeExposure() { return 1.05f; }
    default float getMmeRoughness() { return 0.5f; }
    default float getMmeSpecularIntensity() { return 0.85f; }
    default float getMmeSoftShadow() { return 0.7f; }
    default float getMmeRimPower() { return 3.2f; }
    default float getMmeRimIntensity() { return 0.18f; }
    default float getMmeSubsurface() { return 0.45f; }
    default float getMmeHairAnisotropy() { return 0.65f; }
    default float getMmeFillLight() { return 0.35f; }
}
