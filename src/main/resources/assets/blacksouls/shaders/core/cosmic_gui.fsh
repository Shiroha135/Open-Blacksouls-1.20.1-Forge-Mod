#version 150
#define M_PI 3.1415926535897932384626433832795

const int cosmiccount = 10;
const int cosmicoutof = 101;

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float time;
uniform float opacity;
uniform float scale;
uniform mat2 cosmicuvs[cosmiccount];

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 fPos;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(p + vec3(0, 0, 0)), hash(p + vec3(1, 0, 0)), f.x),
                   mix(hash(p + vec3(0, 1, 0)), hash(p + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(hash(p + vec3(0, 0, 1)), hash(p + vec3(1, 0, 1)), f.x),
                   mix(hash(p + vec3(0, 1, 1)), hash(p + vec3(1, 1, 1)), f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    for (int i = 0; i < 5; i++) {
        value += amplitude * noise(p * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

vec3 getNebula(vec3 rayDir, float currentTime) {
    vec3 p = rayDir * 2.0 + vec3(currentTime * 0.0001);
    float density = pow(fbm(p * 1.5), 2.0);
    vec3 c1 = vec3(1.00, 0.00, 1.00);
    vec3 c2 = vec3(0.00, 1.00, 1.00);
    vec3 c3 = vec3(1.00, 1.00, 0.00);
    float m1 = fbm(p * 2.0 + vec3(100.0));
    float m2 = fbm(p * 1.5 + vec3(200.0));
    vec3 nebulaColor = mix(mix(c1, c2, m1), c3, m2);
    return nebulaColor * density * 0.3;
}

void main() {
    vec4 col = vec4(0.96, 0.46, 1.00, 1.0);
    vec4 dir = normalize(vec4(-fPos, 0));
    col.rgb += getNebula(dir.xyz, time);

    int uvtiles = 16;
    for (int i = 0; i < 16; i++) {
        int mult = 16 - i;
        int j = i + 7;
        float rand1 = (j * j * 4321 + j * 8) * 2.0;
        float rand2 = ((j + 1) * (j + 1) * (j + 1) * 239 + (j + 1) * 37) * 3.6;
        float rawu = 0.5 + (atan(dir.z, dir.x) / (2.0 * M_PI));
        float rawv = 0.5 + (asin(dir.y) / M_PI);

        float effectiveScale = (mult * 0.5 + 2.75) * scale;
        float u = rawu * effectiveScale;
        float v = (rawv + time * 0.0002) * effectiveScale * 0.6;
        int tu = int(mod(floor(u * uvtiles), uvtiles));
        int tv = int(mod(floor(v * uvtiles), uvtiles));
        int pos = ((171 * tu) + (489 * tv) + (303 * (i + 31)) + 17209) ^ 10;
        int symbol = int(mod(pos, cosmicoutof));

        if (symbol >= 0 && symbol < cosmiccount) {
            float ru = clamp(mod(u, 1.0) * uvtiles - tu, 0.0, 1.0);
            float rv = clamp(mod(v, 1.0) * uvtiles - tv, 0.0, 1.0);

            vec2 cosmictex;
            cosmictex.x = cosmicuvs[symbol][0][0] * (1.0 - ru) + cosmicuvs[symbol][1][0] * ru;
            cosmictex.y = cosmicuvs[symbol][0][1] * (1.0 - rv) + cosmicuvs[symbol][1][1] * rv;
            vec4 tcol = texture(Sampler0, cosmictex);
            float a = tcol.r * (0.5 + (1.0 / mult)) * (1.0 - smoothstep(0.15, 0.48, abs(rawv - 0.5)));
            vec3 starColor = vec3(1.0, 0.7 + sin(time * 0.003 + rand1 * 0.1) * 0.15, 1.0);
            col += vec4(starColor, 1.0) * a;
        }
    }

    col.rgb *= vertexColor.rgb;
    col.a *= opacity;
    fragColor = clamp(col * ColorModulator, 0.0, 1.0);
}
