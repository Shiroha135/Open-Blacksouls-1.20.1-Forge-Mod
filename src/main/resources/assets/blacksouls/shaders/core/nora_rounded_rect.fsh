#version 150

uniform vec2 PanelSize;
uniform float Radius;
uniform float StrokeWidth;
uniform float StrokeOnly;

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
    vec2 point = texCoord0 * PanelSize;
    float outer = coverage(roundedDistance(point, PanelSize, Radius));
    float mask = outer;
    if (StrokeOnly > 0.5) {
        vec2 inset = vec2(StrokeWidth);
        vec2 innerSize = max(PanelSize - inset * 2.0, vec2(0.001));
        float innerRadius = max(Radius - StrokeWidth, 0.0);
        float inner = coverage(roundedDistance(point - inset, innerSize, innerRadius));
        mask = clamp(outer - inner, 0.0, 1.0);
    }
    float alpha = vertexColor.a * mask;
    if (alpha <= 0.001) discard;
    fragColor = vec4(vertexColor.rgb, alpha);
}
