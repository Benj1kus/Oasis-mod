#version 150
uniform sampler2D Sampler0;
in vec2 uv;
out vec4 fragColor;
void main() {
    float center = texture(Sampler0, uv).a;
    vec2 stepUV = 4.0 / vec2(textureSize(Sampler0, 0));
    float edge = 0.0;
    for (int y = -1; y <= 1; ++y)
        for (int x = -1; x <= 1; ++x) {
            vec2 delta = vec2(x,y);
            edge = max(edge, texture(Sampler0, uv + delta * stepUV / max(1.0,length(delta))).a);
        }
    float alpha = smoothstep(0.08, 0.65, edge) * (1.0 - smoothstep(0.04, 0.5, center));
    if (alpha < 0.015) discard;
    fragColor = vec4(1.0, 0.79, 0.26, alpha);
}
