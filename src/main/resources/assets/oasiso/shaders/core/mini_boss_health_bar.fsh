#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Flash;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 texel = texture(Sampler0, texCoord);
    float alpha = texel.a * ColorModulator.a;
    if (alpha < 0.0039) discard;
    // Настоящее побеление вместо умножения цветной текстуры на белый.
    vec3 color = mix(texel.rgb, vec3(1.0), clamp(Flash, 0.0, 1.0));
    fragColor = vec4(color * ColorModulator.rgb, alpha);
}
