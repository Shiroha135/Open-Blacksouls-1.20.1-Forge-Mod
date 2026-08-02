#version 150

in vec4 vertexColor;
in vec2 fogCoord;

uniform float GameTime;

out vec4 fragColor;

float hash21(vec2 p) {
    p = mod(p, 289.0);
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise21(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;

    for (int i = 0; i < 5; i++) {
        value += noise21(p) * amplitude;
        p = mat2(1.55, 1.18, -1.18, 1.55) * p + vec2(7.13, 3.71);
        amplitude *= 0.5;
    }

    return value;
}

void main() {
    vec2 uv = fogCoord;

    // Keep animation coordinates small even if the Java side sends absolute ticks.
    float time = mod(abs(GameTime) * 600.0, 256.0);

    // Broad, slow domain warp: this makes rolling cloud banks instead of lines.
    vec2 p = vec2((uv.x - 0.5) * 3.2, uv.y * 3.0);
    vec2 warp;
    warp.x = fbm(p * 0.72 + vec2(time * 0.018, -time * 0.030));
    warp.y = fbm(p * 0.72 + vec2(4.6, 8.2) + vec2(-time * 0.014, -time * 0.024));

    float broad = fbm(p + (warp - 0.5) * 2.1 + vec2(0.0, -time * 0.055));
    float middle = fbm(p * 2.15 + (warp.yx - 0.5) * 1.45 + vec2(time * 0.025, -time * 0.105));
    float detail = fbm(p * 4.5 + (warp - 0.5) * 0.85 + vec2(-time * 0.035, -time * 0.19));

    // Stronger contrast than the previous version so it cannot collapse to a flat slab.
    float density = broad * 0.58 + middle * 0.29 + detail * 0.13;
    density = clamp((density - 0.26) * 1.75, 0.0, 1.0);

    float body = smoothstep(0.08, 0.82, density);
    float brightBillows = smoothstep(0.48, 0.90, density);
    float softVoids = smoothstep(0.18, 0.58, middle);

    vec3 darkFog = vec3(0.34, 0.36, 0.39);
    vec3 midFog = vec3(0.67, 0.70, 0.74);
    vec3 paleFog = vec3(0.96, 0.97, 0.99);

    vec3 color = mix(darkFog, midFog, body);
    color = mix(color, paleFog, brightBillows * 0.68);
    color *= 1.12;

    // Preserve some depth and holes while still blocking the doorway visually.
    float alpha = 0.62 + body * 0.28 + brightBillows * 0.055;
    alpha -= (1.0 - softVoids) * 0.055;
    alpha = clamp(alpha, 0.54, 0.95) * vertexColor.a;

    // Do not let dark world lighting crush the fog into a nearly black rectangle.
    vec3 lightTint = mix(vec3(1.0), vertexColor.rgb, 0.12);
    fragColor = vec4(color * lightTint, alpha);
}
