#version 150
uniform sampler2D Sampler0;
in vec2 texCoord;
in float modelHeight;
out vec4 fragColor;
void main() {
    if (texture(Sampler0, texCoord).a < 0.1) discard;
    fragColor = vec4(clamp(modelHeight, 0.0, 1.0), 0.0, 0.0, 1.0);
}
