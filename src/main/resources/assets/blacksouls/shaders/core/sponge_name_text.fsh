#version 150

uniform sampler2D Sampler0;
uniform float GameTime;

in vec2 texCoord0;

out vec4 fragColor;

vec3 palette(float t) {
    vec3 c1 = vec3(0.55, 0.34, 1.00);
    vec3 c2 = vec3(1.00, 0.48, 0.92);
    vec3 c3 = vec3(0.63, 0.77, 1.00);
    float p1 = smoothstep(0.00, 0.55, t);
    float p2 = smoothstep(0.45, 1.00, t);
    vec3 first = mix(c1, c2, p1);
    return mix(first, c3, p2);
}

void main() {
    vec4 mask = texture(Sampler0, texCoord0);
    if (mask.a <= 0.01) {
        discard;
    }

    float sweep = fract(texCoord0.x * 1.2 - GameTime * 0.35 + texCoord0.y * 0.08);
    float shimmer = sin((texCoord0.x * 11.0 - GameTime * 4.2) * 3.1415926) * 0.5 + 0.5;
    vec3 base = palette(sweep);
    vec3 highlight = mix(base, vec3(1.0), smoothstep(0.65, 1.0, shimmer) * 0.45);
    fragColor = vec4(highlight, mask.a);
}
