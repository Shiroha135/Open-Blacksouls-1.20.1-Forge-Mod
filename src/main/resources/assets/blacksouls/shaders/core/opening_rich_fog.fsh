#version 150

uniform sampler2D Sampler0;
uniform float Time;
uniform vec2 CanvasSize;
uniform vec2 CameraOffset;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float frame = mod(Time * 60.0, 150.0);
    float age = mod(frame, 50.0);
    vec3 opacity;
    if (frame < 50.0) opacity = vec3(100.0 - age * 2.0, 50.0 + age, age);
    else if (frame < 100.0) opacity = vec3(age, 100.0 - age * 2.0, 50.0 + age);
    else opacity = vec3(50.0 + age, age, 100.0 - age * 2.0);
    vec2 pixels = texCoord * CanvasSize + CameraOffset + vec2(Time * 12.0);
    vec2 size = vec2(816.0, 624.0);
    float a = texture(Sampler0, fract(pixels / size)).a * opacity.x / 255.0;
    float b = texture(Sampler0, fract((pixels + vec2(271.0, 143.0)) / size)).a * opacity.y / 255.0;
    float c = texture(Sampler0, fract((pixels + vec2(553.0, 397.0)) / size)).a * opacity.z / 255.0;
    fragColor = vec4(vec3(0.58), 1.0 - (1.0 - a) * (1.0 - b) * (1.0 - c));
}
