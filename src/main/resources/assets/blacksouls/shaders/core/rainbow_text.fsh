#version 150

uniform sampler2D Sampler0;
uniform float GameTime;

in vec2 texCoord0;

out vec4 fragColor;

vec3 hsv2rgb(vec3 c) {
    vec4 k = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + k.xyz) * 6.0 - k.www);
    return c.z * mix(k.xxx, clamp(p - k.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec4 mask = texture(Sampler0, texCoord0);
    if (mask.a <= 0.01) {
        discard;
    }

    float wave = sin((texCoord0.x * 7.5 - GameTime * 2.8) * 3.1415926) * 0.5 + 0.5;
    float hue = fract(GameTime * 0.14 + texCoord0.x * 0.85 + texCoord0.y * 0.18);
    vec3 rainbow = hsv2rgb(vec3(hue, 0.9, 1.0));
    vec3 highlight = mix(rainbow, vec3(1.0), wave * 0.45);
    float glowBand = smoothstep(0.35, 0.9, wave);
    vec3 finalColor = mix(rainbow, highlight, glowBand);

    fragColor = vec4(finalColor, mask.a);
}
