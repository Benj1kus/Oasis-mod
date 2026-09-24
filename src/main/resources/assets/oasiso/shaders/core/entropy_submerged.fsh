#version 150

in vec2 uv;
uniform float Time;
uniform float Strength;
uniform float Aspect;
out vec4 fragColor;

const float PIXEL_HEIGHT = 270.0;
const float WAVE_SPEED = 2.0;
const float WAVE_WIDTH = 3.5;
const float FOAM_AMOUNT = 2.0;
const float OPACITY = 0.88;
const float BUBBLE_PERIOD = 6.5;
const float BUBBLE_SIZE = 3.0;
const float BUBBLE_TRAVEL = 0.18;

const float TAU = 6.2831853;

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

vec3 waterPalette(float light, float threshold) {
    float ramp = clamp(light, 0.0, 0.999) * 5.0;
    float band = floor(ramp) + step(threshold, smoothstep(0.22, 0.78, fract(ramp)));
    if (band < 1.0) return vec3(0.200, 0.040, 0.180);
    if (band < 2.0) return vec3(0.320, 0.100, 0.280);
    if (band < 3.0) return vec3(0.080, 0.300, 0.480);
    if (band < 4.0) return vec3(0.200, 0.750, 0.920);
    if (band < 5.0) return vec3(0.500, 0.920, 0.980);
    return vec3(0.950, 0.990, 1.000);
}

float frameHeight(float x, float seed, float t) {
    float drift = x + t * (0.018 + 0.006 * sin(seed));
    return (0.044 + sin(drift * 10.0 + t * 0.93 + seed) * 0.014
        + sin(drift * 29.0 - t * 1.45 + seed * 2.0) * 0.006) * WAVE_WIDTH;
}

float cover(float distance, float pixel) {
    return clamp(0.5 - distance / (pixel * 2.0), 0.0, 1.0);
}

vec4 over(vec4 back, vec4 front) {
    return front + back * (1.0 - front.a);
}

vec4 paintWater(float distance, float shade, float lip, float pixel, float threshold) {
    if (cover(distance, pixel) <= threshold) return vec4(0.0);
    float foam = (1.0 - smoothstep(pixel * 0.6, pixel * 2.0,
        abs(distance + pixel * 0.3))) * lip * FOAM_AMOUNT;
    float shadow = (1.0 - smoothstep(pixel, pixel * 2.5,
        abs(distance + pixel * 3.2))) * lip;
    vec3 color = waterPalette(shade - shadow * 0.25, threshold);
    if (foam > 0.17 + threshold * 0.36) color = vec3(0.82, 0.42, 0.72);
    if (foam > 0.39 + threshold * 0.43) color = vec3(0.60, 0.94, 1.00);
    if (foam > 0.80 + threshold * 0.19) color = vec3(0.95, 0.99, 1.00);
    float alpha = OPACITY * (0.80 + 0.20 * smoothstep(0.0, 0.6, foam));
    return vec4(color * alpha, alpha);
}

vec4 bubble(vec2 p, float anchor, float random, float phase, float period,
            float seed, float pixel, float threshold) {

    float age = fract(Time / period + phase);

    if (age >= 0.97) return vec4(0.0);
    float growth = smoothstep(0.0, 0.18, age);
    float radius = (0.020 + 0.020 * random) * BUBBLE_SIZE * growth;
    if (radius < pixel * 0.45) return vec4(0.0);

    float t = Time * WAVE_SPEED;
    float releaseTime = Time - max(age - 0.24, 0.0) * period;
    float surface = frameHeight(anchor, seed, releaseTime * WAVE_SPEED);
    float currentSurface = frameHeight(anchor, seed, t);
    float travel = smoothstep(0.24, 0.81, age);
    float rise = smoothstep(0.04, 0.34, age);
    float sway = sin(age * 9.0 + random * TAU) * 0.018 * travel;
    vec2 center = vec2(anchor + sway,
        surface + radius * (-0.60 + rise * 1.80) + BUBBLE_TRAVEL * travel);
    float wobble = sin(age * 21.0 + random * TAU) * 0.075;
    vec2 ellipse = vec2(1.0 + wobble, 1.0 / (1.0 + wobble));

    float bound = radius * 2.5 + pixel * 3.0;
    float lowerBound = age < 0.34 ? min(currentSurface - pixel * 3.0, center.y - bound)
        : center.y - bound;
    if (abs(p.x - center.x) > bound + 0.020 || p.y < lowerBound || p.y > center.y + bound)
        return vec4(0.0);
    vec2 q = (p - center) / ellipse;

    vec4 result = vec4(0.0);
    if (age < 0.34) {
        float neckFade = 1.0 - smoothstep(0.28, 0.34, age);
        float neckRadius = radius * 0.52 * (1.0 - smoothstep(0.14, 0.34, age));
        float neckLength = max(center.y - currentSurface, pixel);
        float along = clamp((p.y - currentSurface) / neckLength, 0.0, 1.0);
        float neckX = mix(anchor, center.x, along);
        float neckWidth = neckRadius * (0.60 + abs(along - 0.5) * 1.2);
        float neckDistance = max(abs(p.x - neckX) - neckWidth,
            max(currentSurface - pixel - p.y, p.y - center.y));
        if (cover(neckDistance, pixel) * neckFade > threshold) {
            result = paintWater(neckDistance, 0.60, 0.50, pixel, threshold);
        }
    }

    float pop = smoothstep(0.80, 0.96, age);
    float popFade = 1.0 - smoothstep(0.83, 0.97, age);
    float outerRadius = radius * (1.0 + 0.48 * pop);
    float radial = length(q);
    float distance = radial - outerRadius;
    float filled = 1.0 - smoothstep(0.20, 0.40, age);
    float ringWidth = pixel * mix(1.7, 0.65, pop);
    float ring = cover(abs(distance + ringWidth * 0.5) - ringWidth * 0.5, pixel);
    float body = cover(distance, pixel);
    float angle = atan(q.y, q.x);

    float fragments = 0.5 + 0.5 * sin(angle * 7.0 + random * TAU);
    float brokenRing = ring * (1.0 - smoothstep(0.05, 0.9, pop)
        * (1.0 - smoothstep(pop * 0.85, pop * 0.85 + 0.12, fragments)));
    float silhouette = max(brokenRing, body * filled) * popFade;
    float glint = clamp(dot(q / max(radial, pixel), normalize(vec2(-0.55, 0.83))), 0.0, 1.0);
    if (silhouette > threshold) {
        vec3 color = waterPalette(0.59 + 0.23 * glint - 0.14 * (1.0 - glint), threshold);
        if (brokenRing > threshold) {
            color = glint > 0.45 ? vec3(0.40, 0.81, 0.76) : vec3(0.055, 0.52, 0.54);
            if (glint * FOAM_AMOUNT > 0.74 + threshold * 0.3) color = vec3(0.70, 0.94, 0.85);
        }
        float alpha = OPACITY * mix(0.75, 0.95, glint * brokenRing);
        result = over(result, vec4(color * alpha, alpha));
    } else if (body > threshold && age < 0.80) {
        vec3 color = waterPalette(0.50 + glint * 0.18, threshold);
        float alpha = OPACITY * 0.10;
        result = over(result, vec4(color * alpha, alpha));
    }

    if (pop > 0.01) {
        float sector = floor((angle / TAU + 1.0) * 5.0 + 0.5);
        float a = sector / 5.0 * TAU;
        vec2 droplet = vec2(cos(a), sin(a)) * radius * (1.04 + pop * 1.25);
        float dropDistance = length(q - droplet) - pixel * 1.3 * (1.0 - pop);
        if (cover(dropDistance, pixel) * popFade > threshold) {
            float alpha = OPACITY * 0.85;
            result = over(result, vec4(vec3(0.55, 0.90, 0.80) * alpha, alpha));
        }
    }
    return result;
}

vec4 waterEdge(vec2 p, float extent, float seed, float pixel, float threshold) {
    float t = Time * WAVE_SPEED;
    float distance = p.y - frameHeight(p.x, seed, t);
    float drift = p.x + t * (0.018 + 0.006 * sin(seed));
    float depth = p.y / WAVE_WIDTH;
    float shade = 0.40 + 0.16 * sin(drift * 18.0 + depth * 42.0 - t + seed);
    float flow = depth * 125.0 + sin(drift * 17.0 - t * 1.3 + seed) * 1.8;
    float ribbons = pow(0.5 + 0.5 * sin(flow + sin(drift * 35.0 + t)), 6.0);
    shade += ribbons * 0.16 * (1.0 - smoothstep(0.025, 0.075, depth));
    vec4 result = paintWater(distance, shade, 0.6, pixel, threshold);

    const float spacing = 0.28;
    float cell = floor(p.x / spacing);
    for (int i = -1; i <= 1; ++i) {
        float id = cell + float(i);
        float random = hash(id + seed * 19.0);
        float anchor = (id + 0.5) * spacing + (random - 0.5) * 0.045;
        if (anchor < 0.075 || anchor > extent - 0.075) continue;
        float period = BUBBLE_PERIOD * (0.82 + random * 0.36);
        float phase = hash(id * 3.1 + seed * 27.0);
        if (abs(p.x - anchor) < 0.13 * BUBBLE_SIZE + 0.02) {
            result = over(result, bubble(p, anchor, random, phase, period, seed, pixel, threshold));
        }
    }
    return result;
}

void main() {
    if (Strength <= 0.001) { fragColor = vec4(0.0); return; }
    float aspect = max(Aspect, 0.1);
    vec2 raw = uv * vec2(aspect, 1.0);
    float pixel = 1.0 / PIXEL_HEIGHT;
    vec2 grid = floor(raw * PIXEL_HEIGHT);
    vec2 p = (grid + 0.5) * pixel;
    float threshold = bayer4(grid);
    float reach = 0.064 * WAVE_WIDTH + BUBBLE_TRAVEL + 0.17 * BUBBLE_SIZE + pixel * 3.0;
    vec4 result = vec4(0.0);
    if (1.0 - p.y < reach)
        result = waterEdge(vec2(p.x, 1.0 - p.y), aspect, 1.7, pixel, threshold);
    if (p.x < reach)
        result = over(result, waterEdge(vec2(1.0 - p.y, p.x), 1.0, 4.2, pixel, threshold));
    if (aspect - p.x < reach)
        result = over(result, waterEdge(vec2(p.y, aspect - p.x), 1.0, 7.8, pixel, threshold));
    if (p.y < reach)
        result = over(result, waterEdge(vec2(aspect - p.x, p.y), aspect, 10.4, pixel, threshold));

    fragColor = result.a > 0.0
        ? vec4(result.rgb / result.a, result.a * clamp(Strength, 0.0, 1.0))
        : vec4(0.0);
}
