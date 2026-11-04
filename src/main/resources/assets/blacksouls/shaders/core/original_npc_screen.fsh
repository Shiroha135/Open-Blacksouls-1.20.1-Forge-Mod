#version 150

uniform sampler2D Sampler0;
uniform vec4 Tone;
uniform vec4 Flash;
uniform float Brightness;
uniform float Shake;
in vec2 screenUv;
out vec4 fragColor;

void main() {
    vec2 shifted = screenUv + vec2(Shake, 0.0);
    vec3 source = vec3(0.0);
    if (shifted.x >= 0.0 && shifted.x <= 1.0) {
        source = texture(Sampler0, shifted).rgb;
    }
    float gray = dot(source, vec3(0.299, 0.587, 0.114));
    vec3 toned = clamp(mix(source, vec3(gray), Tone.a) + Tone.rgb, 0.0, 1.0);
    fragColor = vec4(mix(toned, Flash.rgb, Flash.a) * Brightness, 1.0);
}
