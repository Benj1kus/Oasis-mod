#version 150

uniform float Time;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;

// Основные настройки. RGB записывается как обычные значения 0..255.
const vec3 CORE = vec3(16.0, 7.0, 25.0) / 255.0;
const vec3 WHITE = vec3(255.0, 253.0, 255.0) / 255.0;
const vec3 MINT = vec3(91.0, 255.0, 207.0) / 255.0;
const vec3 CYAN = vec3(98.0, 227.0, 255.0) / 255.0;
const vec3 OUTER_TOP = vec3(167.0, 111.0, 255.0) / 255.0;
const vec3 OUTER_BOTTOM = vec3(245.0, 118.0, 204.0) / 255.0;
const float SPEED = 5.0;
const vec2 PIXELS = vec2(112.0, 184.0);
const float EDGE_WIDTH = 0.026;

float hash(float n) {
    return fract(sin(n * 127.1 + 311.7) * 43758.5453);
}

float bayer2(vec2 p) {
    p = mod(floor(p), 2.0);
    return 2.0 * p.x + 3.0 * p.y - 4.0 * p.x * p.y;
}

float bayer4(vec2 p) {
    return (4.0 * bayer2(p) + bayer2(floor(p * 0.5)) + 0.5) / 16.0;
}

// Единый угловатый силуэт. Двигаются вершины, а не пиксели внутри:
// поэтому стороны остаются прямыми, как у геометрического пламени на референсе.
float flameDistance(vec2 p, float t) {
    vec2 v[19];
    v[0] = vec2(0.00, 0.025);
    v[1] = vec2(0.54, 0.20);
    v[2] = vec2(0.74, 0.48);
    v[3] = vec2(0.63, 0.94);
    v[4] = vec2(0.48, 0.77);
    v[5] = vec2(0.60, 1.22);
    v[6] = vec2(0.31, 1.60);
    v[7] = vec2(0.23, 1.46);
    v[8] = vec2(0.28, 1.94);
    v[9] = vec2(0.43, 2.17);
    v[10] = vec2(-0.04, 1.80);
    v[11] = vec2(0.02, 1.56);
    v[12] = vec2(-0.22, 1.27);
    v[13] = vec2(-0.36, 1.50);
    v[14] = vec2(-0.33, 1.01);
    v[15] = vec2(-0.62, 1.19);
    v[16] = vec2(-0.52, 0.90);
    v[17] = vec2(-0.78, 0.51);
    v[18] = vec2(-0.51, 0.20);

    for (int i = 1; i < 19; ++i) {
        float h = v[i].y / 2.17;
        float f = float(i);
        v[i].x += h * (0.14 * sin(t * 1.85 - h * 3.4)
                    + 0.047 * sin(t * 3.1 + f * 1.7));
        v[i].y += h * 0.067 * sin(t * 2.25 + f * 0.95);
    }
    float d2 = 100.0;
    float signValue = 1.0;
    for (int i = 0; i < 19; ++i) {
        int j = (i + 18) % 19;
        vec2 edge = v[j] - v[i];
        vec2 rel = p - v[i];
        vec2 closest = rel - edge * clamp(dot(rel, edge) / dot(edge, edge), 0.0, 1.0);
        d2 = min(d2, dot(closest, closest));
        bvec3 side = bvec3(p.y >= v[i].y, p.y < v[j].y,
                          edge.x * rel.y > edge.y * rel.x);
        if (all(side) || all(not(side))) signValue = -signValue;
    }
    return signValue * sqrt(d2);
}

float triangleDistance(vec2 p, vec2 a, vec2 b, vec2 c) {
    vec2 e0 = b-a, e1 = c-b, e2 = a-c;
    vec2 v0 = p-a, v1 = p-b, v2 = p-c;
    vec2 q0 = v0-e0*clamp(dot(v0,e0)/dot(e0,e0),0.0,1.0);
    vec2 q1 = v1-e1*clamp(dot(v1,e1)/dot(e1,e1),0.0,1.0);
    vec2 q2 = v2-e2*clamp(dot(v2,e2)/dot(e2,e2),0.0,1.0);
    float s = sign(e0.x*e2.y-e0.y*e2.x);
    vec2 d = min(min(vec2(dot(q0,q0),s*(v0.x*e0.y-v0.y*e0.x)),
                     vec2(dot(q1,q1),s*(v1.x*e1.y-v1.y*e1.x))),
                     vec2(dot(q2,q2),s*(v2.x*e2.y-v2.y*e2.x)));
    return -sqrt(d.x)*sign(d.y);
}

vec3 outerColor(float y) {
    return mix(OUTER_BOTTOM, OUTER_TOP, clamp(y / 2.3, 0.0, 1.0));
}

void main() {
    vec2 cell = floor(texCoord * PIXELS);
    vec2 uv = (cell + 0.5) / PIXELS;
    vec2 p = vec2((uv.x - 0.5) * 2.0, uv.y * 2.85);
    float t = Time * SPEED + vertexColor.r * 71.0;
    float threshold = bayer4(cell);
    float edge = max(EDGE_WIDTH, fwidth(p.x));
    float outer = flameDistance(p, t);
    float coverage = 1.0 - smoothstep(-edge, edge, outer);
    vec3 color = outerColor(p.y);

    // Вложенные слои: бирюза -> белый -> почти чёрная сердцевина.
    // Числа в vec2 = ширина и высота слоя относительно внешнего огня.
    if (outer < edge) {
        vec2 base = p - vec2(0.0, 0.045);
        float mint = flameDistance(base / vec2(0.76, 0.79), t) * 0.76;
        float white = flameDistance(base / vec2(0.58, 0.64), t) * 0.58;
        float core = flameDistance(base / vec2(0.39, 0.49), t) * 0.39;
        if (1.0 - smoothstep(-edge, edge, mint) > threshold)
            color = mix(MINT, CYAN, clamp(p.y / 1.8, 0.0, 1.0));
        if (mint < 0.0 && 1.0 - smoothstep(-edge, edge, white) > threshold)
            color = WHITE;
        if (white < 0.0 && 1.0 - smoothstep(-edge, edge, core) > threshold)
            color = CORE;
    }

    // Шесть постоянных циклов осколков. Начинаются внутри языка,
    // вылетают вверх/вбок, сужаются и растворяются через дизеринг.
    // Это часть одного шейдера: игровые Particle/Entity не создаются.
    for (int i = 0; i < 6; ++i) {
        float id = float(i);
        float life = fract(t * (0.29 + 0.025 * mod(id, 3.0)) + id * 0.173);
        float cycle = floor(t * (0.29 + 0.025 * mod(id, 3.0)) + id * 0.173);
        float random = hash(cycle + id * 13.3 + vertexColor.r * 19.0);
        float side = mod(id, 2.0) * 2.0 - 1.0;
        vec2 start;
        if (i < 2) start = vec2(0.27, 1.82);
        else if (i < 4) start = vec2(-0.36, 1.14);
        else start = vec2(0.50, 1.01);
        float h = start.y / 2.17;
        start.x += h * 0.14 * sin(t * 1.85 - h * 3.4);
        vec2 center = start + vec2(side * life * (0.19 + random * 0.13),
                                 life * (0.60 + random * 0.23));
        float size = (0.060 + random * 0.043) * (1.0 - life * 0.63);
        vec2 q = p - center;
        float angle = side * (0.25 + life * 1.65);
        q = mat2(cos(angle), -sin(angle), sin(angle), cos(angle)) * q;
        float shard = triangleDistance(q, vec2(-size, -size * 0.55),
                        vec2(size * 0.8, -size * 0.3), vec2(size * 0.38, size * 1.75));
        float fade = 1.0 - smoothstep(0.55, 1.0, life);
        float shardCoverage = (1.0 - smoothstep(-edge, edge, shard)) * fade;
        if (shardCoverage > threshold && shardCoverage > coverage) {
            color = outerColor(center.y);
            if (i == 3) color = MINT;
            coverage = shardCoverage;
        }
    }

    // Отброшенные пиксели НЕ записывают глубину. Прозрачного прямоугольника нет.
    if (coverage * vertexColor.a <= threshold) discard;
    fragColor = vec4(color, 1.0);
}
