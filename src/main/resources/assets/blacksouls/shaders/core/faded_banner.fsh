#version 150

in vec2 texCoord0;

uniform float Opacity;

out vec4 fragColor;

void main() {
    float vertical = smoothstep(0.0, 0.16, min(texCoord0.y, 1.0 - texCoord0.y));
    float alpha = 0.733333 * vertical * Opacity;
    fragColor = vec4(0.0, 0.0, 0.0, alpha);
}
