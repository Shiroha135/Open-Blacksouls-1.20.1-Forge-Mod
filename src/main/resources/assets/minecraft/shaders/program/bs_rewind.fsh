#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec2 InSize;
uniform float time;
uniform float Contrast;
uniform float Saturation;

in vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec2 aspect = vec2(OutSize.x / max(OutSize.y, 1.0), 1.0);
    vec2 centered = (texCoord - 0.5) * aspect;
    float dist = length(centered);
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 wave = vec2(
            sin((texCoord.y + time * 2.0) * 18.0),
            cos((texCoord.x - time * 1.5) * 14.0)
    ) * texel * 0.35;

    vec3 base = texture(DiffuseSampler, texCoord + wave).rgb;
    float gray = luminance(base);
    gray = clamp((gray - 0.5) * (Contrast + 0.06) + 0.5, 0.0, 1.0);

    vec3 softGray = vec3(gray) * vec3(0.96, 0.975, 1.0);
    vec3 monochrome = mix(softGray, base, clamp(Saturation, 0.0, 1.0));
    float vignette = smoothstep(0.50, 1.32, dist);
    monochrome *= (1.0 - vignette * 0.08);
    monochrome += vec3(0.015, 0.018, 0.022) * (1.0 - vignette) * 0.25;

    fragColor = vec4(clamp(monochrome, 0.0, 1.0), 1.0);
}
