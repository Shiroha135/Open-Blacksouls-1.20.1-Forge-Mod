#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;
uniform vec2 InSize;
uniform vec2 OutSize;

varying vec2 texCoord;
uniform float BlendFactor;

void main() {
    vec4 current = texture2D(DiffuseSampler, texCoord);
    vec4 previous = texture2D(PrevSampler, texCoord);
    float sizeScale = clamp(min(InSize.x / max(OutSize.x, 1.0), InSize.y / max(OutSize.y, 1.0)), 0.0, 1.0);
    gl_FragColor = mix(current, previous, BlendFactor * sizeScale);
    gl_FragColor.a = 1.0;
}
