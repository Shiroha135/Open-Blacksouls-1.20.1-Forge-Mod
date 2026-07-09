#version 150

uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

vec3 hsv2rgb(vec3 c) {
    vec4 k = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + k.xyz) * 6.0 - k.www);
    return c.z * mix(k.xxx, clamp(p - k.xxx, 0.0, 1.0), c.y);
}

float fbm(vec2 uv, float time) {
    float noiseValue = 0.0;
    uv.x += time * 0.5;
    noiseValue += 0.5000 * sin(uv.x * 8.0 - time * 1.2) * cos(uv.y * 5.0 + time);
    noiseValue += 0.2500 * sin(uv.x * 16.0 + time * 0.8) * cos(uv.y * 10.0 - time * 1.5);
    noiseValue += 0.1250 * sin(uv.x * 32.0 - time * 2.0) * cos(uv.y * 20.0 + time * 2.0);
    return noiseValue * 0.5 + 0.5;
}

void main() {
    float time = GameTime * 1.5;

    float cylinderDist = abs(texCoord0.y - 0.5) * 2.0;
    float bevel = 1.0 - pow(cylinderDist, 2.5);
    float sharpHighlight = smoothstep(0.85, 0.95, 1.0 - texCoord0.y)
        * smoothstep(1.0, 0.95, 1.0 - texCoord0.y);

    vec2 distortedUV = texCoord0;
    distortedUV.y += sin(texCoord0.x * 12.0 - time * 2.0) * 0.06;
    float energy = fbm(distortedUV, time);

    float tipGlow = exp(-(1.0 - texCoord0.x) * 25.0);
    float surge = pow(sin(texCoord0.x * 6.0 - time * 4.0) * 0.5 + 0.5, 8.0) * 0.6;
    float sparkle = pow(energy, 4.0);

    vec3 baseColor = clamp(vertexColor.rgb, 0.0, 1.0);
    bool isRainbowBar = vertexColor.g > 0.95 && vertexColor.r < 0.2 && vertexColor.b < 0.2;

    vec3 finalColor;
    if (isRainbowBar) {
        float hue = fract(time * 0.5 - texCoord0.x * 1.5);
        vec3 rgbGlow = hsv2rgb(vec3(hue, 1.0, 1.0));
        float frostNoise = fract(sin(dot(texCoord0.xy, vec2(12.9898, 78.233))) * 43758.5453);

        vec3 color = mix(rgbGlow, vec3(1.0), frostNoise * 0.2);
        color *= bevel;
        color += rgbGlow * tipGlow * 1.5;
        color += vec3(1.0) * sharpHighlight * 0.8;
        finalColor = color;
    } else {
        vec3 deepTone = baseColor * 0.18;
        vec3 brightTone = mix(baseColor, vec3(1.0), 0.35);
        vec3 accentTone = mix(baseColor, vec3(1.0), 0.65);
        float luminance = dot(baseColor, vec3(0.2126, 0.7152, 0.0722));

        vec3 color = mix(deepTone, brightTone, energy);
        color += accentTone * sparkle * 0.45;
        color *= (0.55 + bevel * 0.45);
        color += accentTone * tipGlow * 1.1;
        color += brightTone * surge * 0.35;
        color += vec3(1.0) * sharpHighlight * (0.25 + luminance * 0.35);
        finalColor = color;
    }

    fragColor = vec4(finalColor, vertexColor.a * (0.8 + bevel * 0.2));
}
