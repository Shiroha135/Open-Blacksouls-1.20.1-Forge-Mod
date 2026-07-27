#version 150

uniform sampler2D Sampler0;
uniform vec2 FrameSize;
uniform float BlurAlpha;
uniform vec2 PanelSize;
uniform float Radius;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float roundedDistance(vec2 point, vec2 size, float radius) {
    vec2 halfSize = size * 0.5;
    vec2 q = abs(point - halfSize) - max(halfSize - vec2(radius), vec2(0.0));
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

float coverage(float distanceValue) {
    float aa = max(fwidth(distanceValue), 0.35);
    return 1.0 - smoothstep(-aa, aa, distanceValue);
}

void main() {
    float mask = coverage(roundedDistance(texCoord0 * PanelSize, PanelSize, Radius));
    if (mask <= 0.001) discard;
    vec2 screenUv = gl_FragCoord.xy / max(FrameSize, vec2(1.0));
    vec3 blurred = texture(Sampler0, screenUv).rgb;
    fragColor = vec4(blurred, mask * BlurAlpha) * vertexColor;
}
