#version 150

uniform float GameTime;
uniform float Opacity;

in vec2 texCoord0;
in vec4 vertexColor;
in vec2 localPos;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), f.x),
    mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 p = localPos;
    float time = GameTime * 0.08;

    // 底色草莓冰淇淋粉
    vec3 basePink   = vec3(0.95, 0.40, 0.75);
    // 中间色换成清透的樱花粉
    vec3 sweetPink  = vec3(1.00, 0.65, 0.85);
    // 纯白高光
    vec3 cloudWhite = vec3(1.00, 0.98, 1.00);

    float n1 = fbm(p * 2.5 + vec2(time * 1.2, time * 0.6));
    float n2 = fbm(p * 4.0 - vec2(time * 0.8, time * 1.0));

    float mask1 = smoothstep(0.15, 0.75, n1);
    // 生成白云
    float mask2 = smoothstep(0.35, 0.80, n2);

    vec3 nebula = mix(basePink, sweetPink, mask1);
    // 棉花质感
    nebula = mix(nebula, cloudWhite, mask2 * 0.88);

    // 闪烁群星
    vec3 stars = vec3(0.0);
    for (int i = 1; i <= 3; i++) {
        float layer = float(i);
        vec2 move = p * (2.0 + layer * 1.5) + vec2(time * 0.8, time * 0.5) / layer;
        vec2 grid = floor(move);
        vec2 cell = fract(move) - 0.5;

        float rnd = hash(grid + layer * 10.0);
        if (rnd > 0.85) {
            float d = length(cell);
            float starCore = smoothstep(0.25, 0.0, d);
            float twinkle = 0.5 + 0.5 * sin(time * 18.0 + rnd * 100.0);
            vec3 starColor = mix(vec3(1.0, 0.8, 0.9), vec3(1.0), rnd);
            stars += starColor * starCore * twinkle * (1.1 / layer);
        }
    }

    vec3 finalColor = nebula + stars;

    // 边缘修正
    float band = 1.0 - abs(texCoord0.y * 2.0 - 1.0);
    finalColor *= mix(0.85, 1.0, smoothstep(0.0, 0.6, band));
    float edgeMask = smoothstep(0.05, 0.35, band);
    // 避免白云叠星星导致色彩截断
    finalColor = clamp(finalColor, 0.0, 1.0);

    fragColor = vec4(finalColor * vertexColor.rgb, edgeMask * vertexColor.a * Opacity);
}