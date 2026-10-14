#version 150
uniform float Time;
uniform float Seed;
uniform float Layer;
uniform float Amount;
in vec3 localPosition;
in vec3 worldNormal;
in vec3 viewDirection;
in float rippleLight;
out vec4 fragColor;

float hash(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}
float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1,0,0)), f.x),
                   mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
                   mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z);
}

float bayer(vec2 cell) {
    ivec2 q = ivec2(mod(cell, 4.0));
    int table[16] = int[16](0,8,2,10, 12,4,14,6, 3,11,1,9, 15,7,13,5);
    return (float(table[q.y * 4 + q.x]) + .5) / 16.0;
}
vec3 palette(float band) {
    if (band < 1.0) return vec3(0.25, 0.05, 0.55);
    if (band < 2.0) return vec3(0.50, 0.10, 0.80);
    if (band < 3.0) return vec3(0.75, 0.20, 0.95);
    if (band < 4.0) return vec3(0.95, 0.35, 0.90);
    return vec3(.91, .38, .83);
}
void main() {
    vec3 grid = vec3(40.0, 20.0, 40.0);
    vec3 cell = floor(localPosition * grid);
    vec3 p = (cell + .5) / grid;
    vec3 a = abs(normalize(localPosition));
    vec2 plane = a.y > max(a.x, a.z) ? cell.xz : (a.x > a.z ? cell.zy : cell.xy);
    float dither = bayer(plane);
    float t = Time + Seed * .17;
    vec3 drift = vec3(t * .065, -t * .09, t * .04);
    float cloud = noise(p * 3.5 + drift + Layer * 5.3);
    float detail = noise(p * 8.0 - drift * .6);
    float value = clamp((cloud - .28) * 2.15 + (detail - .5) * .32, 0.0, 1.0);
    float band = clamp(floor(value * 4.0 + dither * .8), 0.0, 4.0);
    vec3 color = palette(band);
    float facing = abs(dot(normalize(worldNormal), normalize(viewDirection)));
    float edge = step(facing, .33);
    float alpha;
    if (Layer > .5) {
        float blobs = step(.58 + (dither - .5) * .12, cloud + detail * .12);
        float fleck = step(.985, hash(cell + Layer * 17.0)) * blobs;
        color = mix(color, vec3(1.0, 0.4, 0.95), fleck);
        alpha = blobs * .12 + fleck * .12;
    } else {
        float dark = 1.0 - step(2.0, band);
        alpha = .27 + dark * .18 + edge * .13;
        color = mix(color, vec3(0.40, 0.10, 0.70), edge * 0.25);
    }
    float wave = floor(clamp(abs(rippleLight) * 2.2, 0.0, 1.0) * 3.0 + dither) / 3.0;
    color = mix(color, vec3(1.0, 0.8, 1.0), wave * 0.75);
    alpha = clamp((alpha + wave * 0.18) * Amount, 0.0, 0.72);
    if (alpha < .003) discard;
    fragColor = vec4(color, alpha);
}
