#version 150

uniform sampler2D Sampler0;
uniform vec4 Flash;
uniform float Brightness;
uniform float Opacity;
in vec2 screenUv;
out vec4 fragColor;

void main() {
    vec4 source = texture(Sampler0, screenUv);
    fragColor = vec4(mix(source.rgb, Flash.rgb, clamp(Flash.a, 0.0, 1.0))
        * clamp(Brightness, 0.0, 1.0), source.a * clamp(Opacity, 0.0, 1.0));
}
