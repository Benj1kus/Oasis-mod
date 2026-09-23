#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 FieldSize;
in vec2 uv;
out vec4 fragColor;
const int SEARCH = 20;
void main() {
    vec2 p = (floor(uv * FieldSize) + .5) / FieldSize;
    float best = 1e6;
    vec4 nearest = vec4(0.0);
    for (int i = -SEARCH; i <= SEARCH; i++) {
        vec2 sampleUV = p + vec2(float(i) / FieldSize.x, 0.0);
        if (any(lessThan(sampleUV, vec2(0))) || any(greaterThanEqual(sampleUV, vec2(1)))) continue;
        float d = float(i*i);
        if (d >= best || texture(Sampler0, sampleUV).a < .5) continue;
        best = d;
        nearest = vec4(sampleUV, texture(Sampler1, sampleUV).r, 1.0);
    }
    fragColor = nearest;
}
