#version 150
#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float time;
uniform float opacity;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 sampled = texture(Sampler0, texCoord0);
    if (sampled.a < 0.02) {
        discard;
    }

    float pulse = 0.72 + 0.28 * sin(time * 0.12);
    float shimmer = 0.88 + 0.12 * sin(time * 0.18 + texCoord0.y * 18.0);
    vec3 goldCore = vec3(1.00, 0.84, 0.34);
    vec3 goldEdge = vec3(1.00, 0.95, 0.70);
    vec3 color = mix(goldCore, goldEdge, clamp(sampled.a, 0.0, 1.0));
    color *= (0.85 + pulse * 0.35) * shimmer;
    color *= vertexColor.rgb;

    float alpha = sampled.a * (0.65 + pulse * 0.35) * opacity;
    vec4 result = vec4(color, alpha) * ColorModulator;
    fragColor = linear_fog(result, vertexDistance, FogStart, FogEnd, FogColor);
}
