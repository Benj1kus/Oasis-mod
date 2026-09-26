#version 150
uniform float Time;
uniform float Seed;
uniform float Spin;
in vec2 uv;
in vec4 vertexColor;
out vec4 fragColor;
float hash(float n) { return fract(sin(n * 127.1 + Seed * 7.31) * 43758.5453); }
float triangle(float x) { return 1.0 - abs(fract(x) * 2.0 - 1.0); }
float bayer(vec2 pixel) {
    ivec2 p = ivec2(mod(pixel, 4.0));
    int table[16] = int[16](0,8,2,10, 12,4,14,6, 3,11,1,9, 15,7,13,5);
    return (float(table[p.y*4+p.x]) + .5) / 16.0;
}
void main() {
    vec2 pixel = floor(uv * 160.0);
    vec2 p = ((pixel + .5) / 160.0 - .5) * 2.0;
    float r = length(p);
    float turn = atan(p.y, p.x) / 6.28318530718 + .5 + Time * Spin * .72;
    float frame = floor(Time * 12.0);
    float sector = mod(floor(turn * 24.0), 24.0);
    float teeth = .12 * triangle(turn * 24.0) + .03 * triangle(turn * 48.0 + .3);
    float jagged = teeth + .04 * hash(sector + frame * 31.0);
    float radius = .69 + jagged;
    float d = abs(r-radius);
    float core = 1.0 - smoothstep(.018, .048, d);
    float fringe = (1.0 - smoothstep(.048, .092, d)) * .24;
    float coverage = max(core, fringe) * vertexColor.a;
    if (coverage <= bayer(pixel)) discard;
    vec3 color = mix(vertexColor.rgb, vec3(.83, 1.0, 1.0), core * .13);
    fragColor = vec4(color, .94);
}
