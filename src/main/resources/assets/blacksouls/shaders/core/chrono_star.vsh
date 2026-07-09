#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec2 StarCenter;
uniform float StarRadius;

out vec2 texCoord0;
out vec4 vertexColor;
out vec2 localPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
    localPos = (Position.xy - StarCenter) / max(StarRadius, 1.0);
}
