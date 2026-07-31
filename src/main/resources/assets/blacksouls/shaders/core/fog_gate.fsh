#version 150

in vec4 vertexColor;
in vec2 fogCoord;

uniform float GameTime;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise21(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
        mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float value = 0.0;
    float weight = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise21(p) * weight;
        p = mat2(1.62, 1.18, -1.18, 1.62) * p + 8.31;
        weight *= 0.5;
    }
    return value;
}

void main() {
    float time = GameTime * 600.0;
    vec2 p = vec2(fogCoord.x * 2.2, fogCoord.y * 4.8 - time * 0.16);
    float broad = fbm(p + vec2(fbm(p * 0.63 + time * 0.025), 0.0));
    float fine = fbm(vec2(fogCoord.x * 5.8 + broad * 0.72, fogCoord.y * 9.5 - time * 0.31));
    float ribbons = pow(1.0 - abs(sin(fogCoord.x * 18.0 + broad * 5.6 - time * 0.42)), 4.0);
    float threads = pow(1.0 - abs(sin(fogCoord.x * 39.0 - fine * 7.0 + time * 0.27)), 8.0);
    float density = clamp(broad * 0.62 + fine * 0.34 + ribbons * 0.45 + threads * 0.24, 0.0, 1.0);

    vec3 shadow = vec3(0.31);
    vec3 mist = vec3(0.73);
    vec3 highlight = vec3(0.96);
    vec3 color = mix(shadow, mist, smoothstep(0.15, 0.85, density));
    color = mix(color, highlight, clamp(ribbons * 0.34 + threads * 0.48, 0.0, 0.72));
    float alpha = mix(0.94, 0.985, smoothstep(0.12, 0.92, density)) * vertexColor.a;
    fragColor = vec4(color * vertexColor.rgb, alpha);
}
