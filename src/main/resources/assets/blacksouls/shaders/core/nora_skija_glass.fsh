#version 150

uniform float Alpha;
uniform float Highlighted;
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
    float vertical = clamp(texCoord0.y, 0.0, 1.0);
    vec3 topColor = vec3(0.98, 0.99, 1.0);
    vec3 bottomColor = vec3(0.82, 0.92, 1.0);
    vec3 fillColor = mix(topColor, bottomColor, vertical);
    float outAlpha = (0.34 + Highlighted * 0.08) * Alpha * mask;
    if (outAlpha <= 0.001) discard;
    fragColor = vec4(fillColor, outAlpha) * vertexColor;
}
