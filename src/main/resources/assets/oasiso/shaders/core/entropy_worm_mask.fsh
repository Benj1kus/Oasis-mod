#version 150
uniform sampler2D Sampler0;
in vec2 uv;
out vec4 fragColor;
void main() {
    if (texture(Sampler0, uv).a < 0.1) discard;
    fragColor = vec4(1.0);
}
