#version 150

uniform sampler2D Sampler0;
uniform vec2 HalfTexelSize;
uniform float Offset;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = (
        texture(Sampler0, texCoord + vec2(-HalfTexelSize.x * 2.0, 0.0) * Offset) +
        texture(Sampler0, texCoord + vec2(-HalfTexelSize.x, HalfTexelSize.y) * Offset) * 2.0 +
        texture(Sampler0, texCoord + vec2(0.0, HalfTexelSize.y * 2.0) * Offset) +
        texture(Sampler0, texCoord + HalfTexelSize * Offset) * 2.0 +
        texture(Sampler0, texCoord + vec2(HalfTexelSize.x * 2.0, 0.0) * Offset) +
        texture(Sampler0, texCoord + vec2(HalfTexelSize.x, -HalfTexelSize.y) * Offset) * 2.0 +
        texture(Sampler0, texCoord + vec2(0.0, -HalfTexelSize.y * 2.0) * Offset) +
        texture(Sampler0, texCoord - HalfTexelSize * Offset) * 2.0
    ) / 12.0;
    fragColor.a = 1.0;
}
