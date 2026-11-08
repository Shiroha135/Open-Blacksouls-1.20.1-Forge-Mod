#version 150
uniform sampler2D Sampler0;
uniform vec2 WindowSize;
uniform float TileSize;
uniform float BackOpacity;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 base = texture(Sampler0, texCoord * 0.5);
    vec2 tiled = fract(texCoord * WindowSize / TileSize);
    vec4 pattern = texture(Sampler0, vec2(tiled.x * 0.5, 0.5 + tiled.y * 0.5));
    vec3 color = mix(base.rgb, pattern.rgb, pattern.a);
    color = clamp(color + vec3(-51.0, -255.0, -255.0) / 255.0, 0.0, 1.0);
    fragColor = vec4(color, BackOpacity);
}
