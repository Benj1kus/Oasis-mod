#version 150
in vec2 uv;
in float opacity;
out vec4 fragColor;
const int bayer[16] = int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
void main() {
    vec2 p = abs(uv * 2.0 - 1.0);
    float d = max(p.x, p.y);
    float edge = min(abs(d-0.53), min(abs(d-0.74), abs(d-0.95)));
    float band = 1.0 - smoothstep(0.019, 0.019 + max(fwidth(d), 0.003), edge);
    ivec2 cell = ivec2(floor(uv * 96.0)) % 4;
    float threshold = (float(bayer[cell.x + 4 * cell.y]) + 0.5) / 16.0;
    if (band * sqrt(opacity) < threshold) discard;
    fragColor = vec4(vec3(1.0), opacity * 0.95);
}
