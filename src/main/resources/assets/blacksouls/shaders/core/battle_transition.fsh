#version 150

uniform sampler2D Sampler0;
uniform float Progress;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float mask = texture(Sampler0, texCoord0).r;
    float alpha = 1.0 - smoothstep(Progress - 0.035, Progress + 0.035, mask);
    fragColor = vec4(0.0, 0.0, 0.0, alpha);
}
