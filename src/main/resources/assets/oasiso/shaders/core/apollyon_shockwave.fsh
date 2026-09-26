#version 150
uniform float Progress;
uniform float Seed;
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

    vec2 pixel = floor(uv * 256.0);
    vec2 p = ((pixel + .5) / 256.0 - .5) * 10.5;
    float distanceFromCenter = length(p);
    float angle = atan(p.y, p.x) / 6.28318530718 + .5;
    float t = clamp(Progress, 0.0, 1.0);
    float expansion = 1.0 - pow(1.0 - t, 3.0);
    float fade = smoothstep(0.0, .055, t) * (1.0 - smoothstep(.55, 1.0, t));
    float frame = floor(t * 26.0);
    float coverage = 0.0;
    vec3 sum = vec3(0.0);
    for (int i=0; i<3; ++i) {
        float layer = float(i);
        float turn = angle + layer * .079 + frame * .007 * (i==1 ? -1.0 : 1.0);
        float teeth = triangle(turn * 24.0);
        float smallTeeth = triangle(turn * 48.0 + .3);
        float sector = mod(floor(turn * 24.0), 24.0);
        float jagged = .34 * teeth + .11 * smallTeeth
                     + .13 * hash(sector + frame * 31.0 + layer * 17.0);
        float radius = expansion * (3.38 + layer * .43 + jagged);
        float width = .13 + .035 * layer;
        float d = abs(distanceFromCenter - radius);
        float core = 1.0 - smoothstep(width * .32, width, d);
        float fringe = (1.0 - smoothstep(width, width + .15, d)) * .22;
        float a = max(core, fringe) * fade;
        vec3 color = i==0 ? vec3(.24, .92, 1.0)
                   : i==1 ? vec3(1.0, .40, .82) : vec3(.59, .28, 1.0);
        color = mix(color, vec3(.78, 1.0, 1.0), core * .25 * (i==0 ? 1.0 : 0.0));
        sum += color * a;
        coverage += a;
    }
    if (coverage * vertexColor.a <= bayer(pixel)) discard;
    fragColor = vec4(sum / max(coverage, .001) * vertexColor.rgb, .94);
}
