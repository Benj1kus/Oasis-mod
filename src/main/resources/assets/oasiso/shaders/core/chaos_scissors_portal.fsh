#version 150

uniform float Time;
uniform float Reveal;
uniform float Despawn;
uniform float Seed;
uniform float GlowOnly;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = 6.28318530718;

const float SHAPE_SPEED = 0.095;
const float ORBIT_SPEED = 0.17;
const float COLOR_SPEED = 0.30;
const float GOLD_ACCENT = 0.65;
const float GLOW_STRENGTH = 1.0;
const float EDGE_WIDTH = 0.0055;
const int MOTE_COUNT = 26;

const vec3 INK  = vec3(0.009, 0.016, 0.048);
const vec3 CYAN = vec3(0.12, 0.92, 1.00);
const vec3 BLUE = vec3(0.29, 0.49, 1.00);
const vec3 JADE = vec3(0.28, 1.00, 0.69);
const vec3 GOLD = vec3(1.00, 0.81, 0.35);

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    return fract(p * (p + p));
}

float random(float id) {
    return hash11(id + Seed * 17.731);
}

mat2 rotation(float angle) {
    float c = cos(angle), s = sin(angle);
    return mat2(c, s, -s, c);
}

vec2 direction(float angle) {
    return vec2(cos(angle), sin(angle));
}

float noise2(vec2 p) {
    vec2 cell = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float seed = random(73.8) * 19.0;
    float a = hash11(dot(cell, vec2(127.1, 311.7)) + seed);
    float b = hash11(dot(cell + vec2(1.0, 0.0), vec2(127.1, 311.7)) + seed);
    float c = hash11(dot(cell + vec2(0.0, 1.0), vec2(127.1, 311.7)) + seed);
    float d = hash11(dot(cell + vec2(1.0, 1.0), vec2(127.1, 311.7)) + seed);
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float clouds(vec2 p) {
    float n = noise2(p) * 0.57;
    p = rotation(0.65) * p * 2.03 + vec2(7.1, 3.6);
    n += noise2(p) * 0.28;
    p = rotation(0.65) * p * 2.03 + vec2(7.1, 3.6);
    return n + noise2(p) * 0.15;
}

float smoothUnion(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * h * k / 6.0;
}

float lobe(vec2 p, vec2 center, float radius, float id) {
    float phase = random(id * 7.31) * TAU;
    center += 0.014 * vec2(sin(Time * 0.43 + phase), cos(Time * 0.37 + phase));
    radius += 0.010 * sin(Time * 0.61 + phase);
    return length(p - center) - radius;
}

float silhouette(vec2 p) {
    p += 0.009 * vec2(sin(p.y * 5.1 + Time * 0.42), cos(p.x * 4.7 - Time * 0.36));
    float d = length(p) - 0.50;
    d = smoothUnion(d, lobe(p, vec2(-0.37,  0.40), 0.28, 1.0), 0.10);
    d = smoothUnion(d, lobe(p, vec2(-0.47,  0.07), 0.20, 2.0), 0.09);
    d = smoothUnion(d, lobe(p, vec2(-0.44, -0.31), 0.24, 3.0), 0.09);
    d = smoothUnion(d, lobe(p, vec2( 0.00, -0.64), 0.25, 4.0), 0.08);
    d = smoothUnion(d, lobe(p, vec2( 0.29, -0.45), 0.21, 5.0), 0.07);
    d = smoothUnion(d, lobe(p, vec2( 0.43, -0.18), 0.22, 6.0), 0.10);
    d = smoothUnion(d, lobe(p, vec2( 0.43,  0.21), 0.21, 7.0), 0.08);
    d = smoothUnion(d, lobe(p, vec2( 0.20,  0.47), 0.23, 8.0), 0.10);
    d = smoothUnion(d, lobe(p, vec2(-0.08,  0.63), 0.21, 9.0), 0.09);
    for (int i = 0; i < 4; ++i) {
        float id = float(i);
        float angle = id * 1.72 + 0.37 + 0.07 * sin(Time * 0.29 + id);
        float radius = 0.038 + random(id * 9.7 + 50.0) * 0.027;
        float orbit = 0.83 + 0.047 * sin(Time * 0.62 + id * 2.31);
        float bead = length(p - direction(angle) * orbit) - radius;
        d = smoothUnion(d, bead, 0.055);
    }
    return d;
}

vec3 edgePalette(vec2 p) {
    float angle = atan(p.y, p.x + 0.000001) - Time * COLOR_SPEED + random(81.3) * TAU;
    vec3 color = mix(CYAN, BLUE, smoothstep(-0.35, 0.82, sin(angle)));
    color = mix(color, JADE, smoothstep(0.40, 0.98, cos(angle + 1.7)) * 0.87);
    return mix(color, GOLD, smoothstep(0.90, 0.997, cos(angle - 0.55)) * GOLD_ACCENT);
}

vec2 spiralPoint(float progress) {
    return direction(PI * 0.5 - progress * TAU * 2.25) * (0.018 + 0.81 * pow(progress, 0.82));
}

float openingDistance(vec2 p, float reveal) {
    float progress = smoothstep(0.015, 0.83, reveal);
    float d = length(p) - 0.022;
    float turn = mod((PI * 0.5 - atan(p.y, p.x + 0.000001)) / TAU, 1.0);
    for (int i = 0; i < 4; ++i) {
        float candidate = (turn + float(i) - 1.0) / 2.25;
        float t = clamp(candidate, 0.0, progress);
        vec2 point = spiralPoint(t);
        float dist = length(p - point);
        float stroke = dist - mix(0.025, 0.10, t);
        d = min(d, stroke);
    }
    float spread = mix(-0.03, 1.16, smoothstep(0.50, 0.995, reveal));
    return min(d, length(p) - spread);
}

void motes(vec2 p, float aa, out vec3 discs, out vec3 halos) {
    discs = vec3(0.0);
    halos = vec3(0.0);
    for (int i = 0; i < MOTE_COUNT; ++i) {
        float id = float(i);
        float h = random(id * 13.37 + 4.2);
        float depth = random(id * 8.91 + 15.0);
        float angle = id * 2.399963 + random(id + 9.0) * 0.65;
        angle -= Time * ORBIT_SPEED * mix(0.65, 1.35, depth);
        float radius = mix(0.12, 0.74, sqrt(h));
        radius += 0.018 * sin(Time * 0.55 + id * 1.9);
        vec2 delta = p - direction(angle) * radius;
        float size = mix(0.009, 0.036, depth * depth);
        float distanceToMote = length(delta);
        float disc = 1.0 - smoothstep(size - aa, size + aa, distanceToMote);
        float pulse = 0.76 + 0.24 * sin(Time * 0.9 + id * 2.7);
        vec3 color = mix(BLUE, CYAN, random(id * 6.81 + 3.0));
        color = mix(color, JADE, smoothstep(0.86, 1.0, random(id * 3.1 + 44.0)) * 0.65);
        float wash = mix(0.58, 0.92, smoothstep(-size, size, delta.y));
        discs += color * disc * pulse * wash;
        halos += color * exp(-dot(delta, delta) / (size * size * 5.0)) * pulse * 0.12;
    }
}

float glint(vec2 delta, vec2 size, float aa) {
    vec2 p = abs(delta) / size;
    float star = pow(p.x, 0.55) + pow(p.y, 0.55);
    return 1.0 - smoothstep(1.0 - aa / min(size.x, size.y),
                            1.0 + aa / min(size.x, size.y), star);
}

vec3 rimGlints(vec2 p, float spin, float aa) {
    vec3 result = vec3(0.0);
    for (int i = 0; i < 3; ++i) {
        float id = float(i);
        vec2 center = rotation(spin) * (direction(id * 2.13 + 0.30) * (0.88 + 0.02 * sin(Time * 0.4 + id)));
        vec2 delta = p - center;
        float pulse = pow(0.5 + 0.5 * sin(Time * 1.35 + id * 2.35), 7.0);
        float star = glint(delta, vec2(0.15, 0.095) * (0.65 + pulse * 0.35), aa);
        result += mix(CYAN, vec3(0.9, 0.98, 1.0), 0.72) * star * pulse;
        result += CYAN * exp(-length(delta) * 62.0) * pulse * 0.35;
    }
    return result;
}

vec3 departingMotes(vec2 p, float despawn, float aa) {
    vec3 result = vec3(0.0);
    for (int i = 0; i < 10; ++i) {
        float id = float(i);
        float phase = clamp((despawn - random(id * 8.1 + 7.0) * 0.3) / 0.7, 0.0, 1.0);
        float life = smoothstep(0.0, 0.16, phase) * (1.0 - smoothstep(0.45, 1.0, phase));
        float angle = id * 2.399963 - phase * 0.65;
        vec2 center = direction(angle) * (0.3 + random(id * 5.9) * 0.42 + phase * 0.20);
        float size = mix(0.025, 0.008, phase);
        float dist = length(p - center);
        float dotMask = 1.0 - smoothstep(size - aa, size + aa, dist);
        result += mix(CYAN, JADE, random(id + 19.0)) * life * (dotMask * 0.72 + exp(-dist * 75.0) * 0.3);
    }
    return result;
}

void main() {
    float reveal = clamp(Reveal, 0.0, 1.0);
    float despawn = clamp(Despawn, 0.0, 1.0);
    if (reveal <= 0.0 || despawn >= 1.0) discard;

    vec2 p = (texCoord - 0.5) * vec2(2.20, 2.695);
    float spin = -Time * SHAPE_SPEED;
    vec2 q = rotation(-spin) * p;
    float d = silhouette(q);

    if (reveal < 0.995) d = max(d, openingDistance(q, reveal));
    if (despawn > 0.001) {
        float breakup = clouds(q * 5.4 + vec2(3.7, 11.2));
        float threshold = mix(-0.15, 1.15, smoothstep(0.0, 1.0, despawn));
        d = max(d, (threshold - breakup) * 0.23);
    }

    float pixel = max(length(fwidth(p)) * 0.55, 0.0008);
    float aa = clamp(fwidth(d) * 0.65, pixel * 0.5, pixel * 1.5);
    float body = 1.0 - smoothstep(-aa, aa, d);
    float line = 1.0 - smoothstep(EDGE_WIDTH - aa, EDGE_WIDTH + aa, abs(d));
    float fade = smoothstep(0.0, 0.065, reveal) * (1.0 - smoothstep(0.86, 1.0, despawn));
    vec2 frame = abs(texCoord * 2.0 - 1.0);
    float frameFade = 1.0 - smoothstep(0.91, 0.995, max(frame.x, frame.y));
    fade *= frameFade;
    vec3 borderColor = edgePalette(q);
    float settled = smoothstep(0.62, 0.99, reveal) * (1.0 - smoothstep(0.0, 0.78, despawn));

    if (GlowOnly > 0.5) {
        float nearGlow = exp(-abs(d) * 68.0) * 0.38;
        float softGlow = exp(-abs(d) * 21.0) * 0.14 * (1.0 - body * 0.72);
        vec3 emission = borderColor * (nearGlow + softGlow);
        if (body > 0.001) {
            vec3 discs, halos;
            motes(p, pixel, discs, halos);
            emission += halos * body * settled;
        }
        emission += rimGlints(p, spin, pixel) * settled;
        if (reveal < 0.94) {
            vec2 head = spiralPoint(smoothstep(0.015, 0.83, reveal));
            float headLife = 1.0 - smoothstep(0.78, 0.94, reveal);
            emission += mix(CYAN, JADE, 0.45) * exp(-length(q - head) * 46.0) * headLife * 0.75;
        }
        if (despawn > 0.0) emission += departingMotes(q, despawn, pixel);
        emission = clamp(emission * fade * GLOW_STRENGTH, 0.0, 1.0);
        float alpha = max(emission.r, max(emission.g, emission.b));
        if (alpha < 0.001) discard;
        fragColor = vec4(emission / alpha, alpha);
        return;
    }

    float alpha = (body * 0.992 * (1.0 - line) + line) * fade;
    if (alpha < 0.001) discard;

    vec2 flow = rotation(-Time * 0.055 - 0.55 * exp(-length(p) * 2.4)) * p;
    float cloudA = clouds(flow * 3.1 + vec2(Time * 0.025, 2.1));
    float cloudB = clouds(rotation(0.9) * flow * 3.7 + vec2(7.4, -Time * 0.022));
    vec3 interior = INK;
    interior += vec3(0.018, 0.030, 0.115) * smoothstep(0.28, 0.76, cloudA);
    interior += vec3(0.008, 0.075, 0.095) * smoothstep(0.42, 0.83, cloudB);
    interior *= 0.76 + 0.24 * smoothstep(0.03, 0.60, length(p));
    interior += borderColor * exp(-abs(d) * 24.0) * 0.085;
    vec3 discs, halos;
    motes(p, pixel, discs, halos);
    interior += discs * settled;
    vec3 edgeCore = mix(borderColor, vec3(0.84, 0.99, 1.0), 0.27);
    vec3 premultiplied = (interior * body * 0.992 * (1.0 - line) + edgeCore * line) * fade;
    fragColor = vec4(clamp(premultiplied / max(alpha, 0.0001), 0.0, 1.0), alpha);
}
