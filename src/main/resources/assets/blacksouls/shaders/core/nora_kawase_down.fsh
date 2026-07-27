#version 150

uniform sampler2D Sampler0;
uniform vec2 HalfTexelSize;
uniform float Offset;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = (
        texture(Sampler0, texCoord) * 4.0 +
        texture(Sampler0, texCoord - HalfTexelSize.xy * Offset) +
        texture(Sampler0, texCoord + HalfTexelSize.xy * Offset) +
        texture(Sampler0, texCoord + vec2(HalfTexelSize.x, -HalfTexelSize.y) * Offset) +
        texture(Sampler0, texCoord - vec2(HalfTexelSize.x, -HalfTexelSize.y) * Offset)
    ) / 8.0;
    fragColor.a = 1.0;
}
