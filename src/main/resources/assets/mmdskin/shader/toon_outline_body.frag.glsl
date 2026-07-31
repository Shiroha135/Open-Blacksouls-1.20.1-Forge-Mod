#version 330 core

in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewPos;

uniform sampler2D Sampler0;
uniform vec3 OutlineColor;
uniform float OutlineAlpha;
uniform float AlphaCutoff;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 fragData1;
layout(location = 2) out vec4 fragData2;
layout(location = 3) out vec4 fragData3;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    float alpha = texColor.a * OutlineAlpha;
    if (alpha < AlphaCutoff) {
        discard;
    }

    vec3 normal = normalize(viewNormal);
    fragColor = vec4(OutlineColor, alpha);
    fragData1 = vec4(normal * 0.5 + 0.5, 1.0);
    fragData2 = vec4(0.0, 0.0, 0.0, 1.0);
    fragData3 = vec4(0.0, 0.0, 0.0, 1.0);
}
